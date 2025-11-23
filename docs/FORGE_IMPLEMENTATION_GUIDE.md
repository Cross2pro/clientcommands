# Forge Implementation Guide for crackPlayerRNG

## Executive Summary
This guide provides a comprehensive roadmap for porting the `crackPlayerRNG` functionality from Fabric to Forge. While the core RNG cracking algorithm remains the same, the implementation differs significantly due to Forge's different architecture, event system, and tooling.

## Architecture Differences: Fabric vs Forge

### Fabric
- **Loader**: Fabric Loader (lightweight, mixin-first)
- **Event System**: Fabric API events (functional interface based)
- **Mixins**: MixinExtras with extensive ASM access
- **Commands**: Brigadier integration via Fabric API
- **Configuration**: Custom solutions (BetterConfig in this case)
- **Lifecycle**: Client/Server separation built-in

### Forge
- **Loader**: NeoForge/MinecraftForge (heavier, hooks-based)
- **Event System**: Event bus (annotation-driven)
- **Mixins**: Limited support, prefer Forge hooks/events
- **Commands**: Built-in Brigadier registration
- **Configuration**: Forge Config API (TOML-based)
- **Lifecycle**: Dist annotation system

## Implementation Strategy

### Phase 1: Project Setup

#### 1.1 Create Forge Mod Structure
```
src/main/
├── java/com/yourname/playerrngcracker/
│   ├── PlayerRNGCrackerMod.java          # Main mod class
│   ├── command/
│   │   └── CrackRNGCommand.java          # Command registration
│   ├── features/
│   │   ├── CCrackRng.java                # Core cracking logic
│   │   ├── CCrackRngGen.java             # LattiCG generated code
│   │   ├── PlayerRandCracker.java        # RNG maintenance
│   │   └── ServerBrandManager.java       # Server detection
│   ├── event/
│   │   └── PlayerRNGEventHandler.java    # Forge event handlers
│   ├── task/
│   │   ├── TaskManager.java              # Async task system
│   │   └── ItemThrowTask.java            # Item throwing logic
│   ├── network/
│   │   └── PacketHandler.java            # Network packet handling
│   └── config/
│       └── PlayerRNGConfig.java          # Forge config
└── resources/
    ├── META-INF/
    │   └── mods.toml                      # Mod metadata
    ├── pack.mcmeta                        # Resource pack info
    └── assets/playerrngcracker/
        └── lang/
            └── en_us.json                 # Translations
```

#### 1.2 Dependencies (build.gradle)
```groovy
dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    
    // LattiCG for RNG cracking
    implementation "com.seedfinding:latticg:${latticg_version}:rt"
    
    // Include in JAR
    shadow "com.seedfinding:latticg:${latticg_version}:rt"
}

shadowJar {
    configurations = [project.configurations.shadow]
    relocate 'com.seedfinding.latticg', 'com.yourname.playerrngcracker.shadow.latticg'
}
```

### Phase 2: Core Components

#### 2.1 Main Mod Class
```java
@Mod("playerrngcracker")
public class PlayerRNGCrackerMod {
    public static final String MOD_ID = "playerrngcracker";
    private static final Logger LOGGER = LogUtils.getLogger();

    public PlayerRNGCrackerMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register configuration
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, 
                                                PlayerRNGConfig.SPEC);
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new PlayerRNGEventHandler());
        
        // Register commands on client setup
        modEventBus.addListener(this::onClientSetup);
        
        // Lifecycle events
        modEventBus.addListener(this::onCommonSetup);
    }
    
    private void onClientSetup(FMLClientSetupEvent event) {
        // Client-only initialization
        event.enqueueWork(() -> {
            PlayerRandCracker.registerEvents();
        });
    }
    
    private void onCommonSetup(FMLCommonSetupEvent event) {
        // Common initialization
        PacketHandler.register();
    }
}
```

#### 2.2 Command Registration (Forge Style)
```java
public class CrackRNGCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ccrackrng")
            .executes(context -> crackPlayerRNG(context.getSource())));
    }
    
    private static int crackPlayerRNG(CommandSourceStack source) 
            throws CommandSyntaxException {
        // Check if on client side
        if (!source.getLevel().isClientSide()) {
            throw new CommandSyntaxException(
                new SimpleCommandExceptionType(() -> "Client-only command"),
                Component.literal("This command can only be run on the client")
            );
        }
        
        ServerBrandManager.rngWarning();
        
        CCrackRng.crack(seed -> {
            source.sendSuccess(() -> Component.translatable(
                "commands.ccrackrng.success", 
                Long.toHexString(seed)
            ), false);
            PlayerRandCracker.setSeed(seed);
            PlayerRNGConfig.playerCrackState.set(
                PlayerRandCracker.CrackState.CRACKED
            );
        });
        
        return Command.SINGLE_SUCCESS;
    }
}
```

**Registration via Event:**
```java
@SubscribeEvent
public static void onRegisterCommands(RegisterClientCommandsEvent event) {
    CrackRNGCommand.register(event.getDispatcher());
}
```

#### 2.3 Configuration System
```java
public class PlayerRNGConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<CrackState> playerCrackState;
    public static final ForgeConfigSpec.BooleanValue playerRNGMaintenance;
    public static final ForgeConfigSpec.BooleanValue infiniteTools;
    public static final ForgeConfigSpec.DoubleValue itemThrowsPerTick;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        
        builder.comment("Player RNG Cracker Settings").push("general");
        
        playerCrackState = builder
            .comment("Current state of player RNG cracking")
            .defineEnum("playerCrackState", CrackState.UNCRACKED);
        
        playerRNGMaintenance = builder
            .comment("Automatically maintain RNG state")
            .define("playerRNGMaintenance", true);
        
        infiniteTools = builder
            .comment("Prevent tool breaking by manipulating RNG")
            .define("infiniteTools", false);
        
        itemThrowsPerTick = builder
            .comment("Number of items to throw per tick")
            .defineInRange("itemThrowsPerTick", 0.2, 0.01, 5.0);
        
        builder.pop();
        SPEC = builder.build();
    }
}
```

### Phase 3: Event System Migration

#### 3.1 Replace Fabric Events with Forge Events

**Fabric Code:**
```java
ClientLevelEvents.LOAD_LEVEL.register(level -> {
    resetCracker(RNGCallType.RECREATED);
});
```

**Forge Equivalent:**
```java
@SubscribeEvent
public static void onWorldLoad(ClientPlayerNetworkEvent.LoggingIn event) {
    PlayerRandCracker.resetCracker(RNGCallType.RECREATED);
}

@SubscribeEvent
public static void onWorldUnload(ClientPlayerNetworkEvent.LoggingOut event) {
    PlayerRandCracker.resetCracker(RNGCallType.RECREATED);
}
```

#### 3.2 Custom Event System
For RNG call detection, create a custom event:

```java
public class PlayerRNGCallEvent extends Event {
    private final RNGCallType type;
    private boolean isMaintained;
    private boolean isMaintainedEvenIfSeedUnknown;
    
    public PlayerRNGCallEvent(RNGCallType type, boolean isMaintained) {
        this.type = type;
        this.isMaintained = isMaintained;
    }
    
    public RNGCallType getType() { return type; }
    
    public void setMaintained() { 
        this.isMaintained = true; 
    }
    
    public void setMaintainedEvenIfSeedUnknown() {
        this.isMaintainedEvenIfSeedUnknown = true;
    }
    
    public boolean isMaintained() { 
        return isMaintained; 
    }
    
    public boolean isMaintainedEvenIfSeedUnknown() {
        return isMaintainedEvenIfSeedUnknown;
    }
}
```

**Event Bus Registration:**
```java
@EventBusSubscriber(modid = MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
public class PlayerRNGEventHandler {
    @SubscribeEvent
    public static void onPlayerRNGCall(PlayerRNGCallEvent event) {
        // Custom logic for RNG call handling
    }
}
```

### Phase 4: Mixin vs Forge Hooks

#### 4.1 Decision Matrix

| Use Case | Fabric Approach | Forge Approach | Recommendation |
|----------|----------------|----------------|----------------|
| Player item drop | Mixin injection | Forge Event (ItemTossEvent) | Use Forge Event |
| Food consumption | Mixin injection | Access Transformer + Reflection | Use AT + Reflection |
| Item damage | Mixin injection | Access Transformer + Hook | Use AT + Hook |
| Entity spawning | Mixin injection | Forge Event (EntityJoinLevelEvent) | Use Forge Event |
| Attack handling | Mixin @WrapOperation | Forge Event (LivingAttackEvent) | Use Forge Event |

#### 4.2 Forge Event Examples

**Item Drop Detection:**
```java
@SubscribeEvent
public static void onItemToss(ItemTossEvent event) {
    if (event.getPlayer() instanceof LocalPlayer) {
        PlayerRandCracker.onDropItem();
    }
}
```

**Entity Spawn (for thrown items):**
```java
@SubscribeEvent
public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity) {
        Level level = event.getLevel();
        ItemEntity itemEntity = (ItemEntity) event.getEntity();
        
        // Check if this is near the player
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.position().distanceToSqr(
                itemEntity.position()) < 1.0) {
            
            if (PlayerRNGConfig.playerCrackState.get() == CrackState.CRACKING) {
                Vec3 motion = itemEntity.getDeltaMovement();
                float speed = (float) Math.sqrt(
                    motion.x * motion.x + motion.z * motion.z
                ) * 50f;
                CCrackRng.onEntityCreation(speed);
            }
        }
    }
}
```

**Living Entity Attack:**
```java
@SubscribeEvent
public static void onLivingAttack(LivingAttackEvent event) {
    if (event.getSource().getEntity() instanceof LocalPlayer player) {
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.isEmpty() && event.getEntity() instanceof LivingEntity target) {
            // Predict weapon damage
            WeaponComponent weaponComponent = weapon.get(DataComponents.WEAPON);
            if (weaponComponent != null) {
                PlayerRandCracker.onItemDamage(
                    weaponComponent.itemDamagePerAttack(),
                    player,
                    weapon
                );
            }
        }
    }
}
```

#### 4.3 When Mixins Are Necessary

Some operations have no Forge hooks and require Mixins or Access Transformers:

**Access Transformer (access_transformer.cfg):**
```
public net.minecraft.world.entity.player.Player f_36103_ # random field
public-f net.minecraft.world.entity.LivingEntity m_21238_(I)I # getJumpPower
```

**Mixin for Complex Cases:**
```java
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"))
    private void onDamage(int amount, LivingEntity entity, 
                         Consumer<Item> breakCallback, CallbackInfo ci) {
        if (entity instanceof LocalPlayer && !entity.level().isClientSide()) {
            PlayerRandCracker.onItemDamage(amount, entity, (ItemStack)(Object)this);
        }
    }
}
```

**Mixin Configuration (mixins.playerrngcracker.json):**
```json
{
  "required": true,
  "package": "com.yourname.playerrngcracker.mixin",
  "compatibilityLevel": "JAVA_17",
  "client": [
    "ItemStackMixin",
    "PlayerMixin",
    "ConsumableMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

**build.gradle Mixin Setup:**
```groovy
minecraft {
    mappings channel: 'official', version: minecraftVersion
    
    runs {
        client {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', 
                     "${projectDir}/build/createSrgToMcp/output.srg"
            arg "-mixin.config=mixins.playerrngcracker.json"
        }
    }
}
```

### Phase 5: Core Algorithm Port

The core RNG cracking algorithm can be ported directly:

#### 5.1 CCrackRng.java
- **No changes needed** to the core logic
- Update event firing mechanism
- Change Fabric command feedback to Forge equivalents

#### 5.2 CCrackRngGen.java
- **Direct copy** - This is generated code from LattiCG
- Ensure LattiCG library is properly shadowed/relocated

#### 5.3 PlayerRandCracker.java
Key changes:
```java
// Fabric version:
public static final Event<RNGCallListener> RNG_CALLED_EVENT = 
    EventFactory.createArrayBacked(RNGCallListener.class, ...);

// Forge version:
public static void fireRNGCalledEvent(RNGCallEvent event) {
    MinecraftForge.EVENT_BUS.post(event);
}

// Usage:
private static boolean canMaintainPlayerRNG(RNGCallType callType) {
    RNGCallEvent event = new RNGCallEvent(
        callType, 
        PlayerRNGConfig.playerRNGMaintenance.get() && 
        PlayerRNGConfig.playerCrackState.get().knowsSeed()
    );
    fireRNGCalledEvent(event);
    
    if (event.isMaintained() && 
        PlayerRNGConfig.playerCrackState.get().knowsSeed()) {
        PlayerRNGConfig.playerCrackState.set(CrackState.CRACKED);
        return true;
    }
    return event.isMaintainedEvenIfSeedUnknown();
}
```

### Phase 6: Network Packet Handling

#### 6.1 Packet Structure
```java
public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("playerrngcracker", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void register() {
        INSTANCE.messageBuilder(EntitySpawnPacket.class, packetId++)
            .encoder(EntitySpawnPacket::encode)
            .decoder(EntitySpawnPacket::decode)
            .consumerMainThread(EntitySpawnPacket::handle)
            .add();
    }
}
```

#### 6.2 Entity Spawn Packet (if needed)
For capturing entity spawn data on client:
```java
public class EntitySpawnPacket {
    private final int entityId;
    private final Vec3 motion;
    
    public EntitySpawnPacket(int entityId, Vec3 motion) {
        this.entityId = entityId;
        this.motion = motion;
    }
    
    public static void encode(EntitySpawnPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeDouble(msg.motion.x);
        buf.writeDouble(msg.motion.y);
        buf.writeDouble(msg.motion.z);
    }
    
    public static EntitySpawnPacket decode(FriendlyByteBuf buf) {
        return new EntitySpawnPacket(
            buf.readInt(),
            new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
        );
    }
    
    public static void handle(EntitySpawnPacket msg, 
                             Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Handle on main thread
            CCrackRng.onEntityCreation(msg.motion);
        });
        ctx.get().setPacketHandled(true);
    }
}
```

### Phase 7: Task Management System

#### 7.1 Forge-Compatible Task Manager
```java
public class TaskManager {
    private static final Map<String, Task> tasks = new HashMap<>();
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Task> entry : tasks.entrySet()) {
            Task task = entry.getValue();
            if (task.condition()) {
                task.onTick();
            } else {
                task.onCompleted();
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(tasks::remove);
    }
    
    public static String addTask(String name, Task task) {
        String uniqueName = name;
        int counter = 1;
        while (tasks.containsKey(uniqueName)) {
            uniqueName = name + "_" + counter++;
        }
        tasks.put(uniqueName, task);
        task.initialize();
        return uniqueName;
    }
}
```

#### 7.2 Item Throw Task Port
Similar structure to Fabric version, but:
- Replace Fabric events with Forge events
- Use Forge config instead of Configs class
- Update chat/feedback methods for Forge

### Phase 8: Testing Strategy

#### 8.1 Unit Tests
```java
public class PlayerRandCrackerTest {
    @Test
    public void testRNGImplementation() {
        PlayerRandCracker.setSeed(12345L);
        int first = PlayerRandCracker.nextInt();
        
        // Reset and verify
        PlayerRandCracker.setSeed(12345L);
        assertEquals(first, PlayerRandCracker.nextInt());
    }
    
    @Test
    public void testSeedCracking() {
        // Generate known float sequence
        Random rand = new Random(67890L ^ PlayerRandCracker.MULTIPLIER);
        float[] floats = new float[10];
        for (int i = 0; i < 10; i++) {
            floats[i] = rand.nextFloat();
        }
        
        // Attempt to recover seed
        long[] seeds = CCrackRngGen.getSeeds(
            floats[0] - 0.001f, floats[0] + 0.001f,
            floats[1] - 0.001f, floats[1] + 0.001f,
            // ... rest of floats
        ).toArray();
        
        assertTrue(seeds.length > 0);
        assertEquals(67890L, seeds[0]);
    }
}
```

#### 8.2 Integration Tests
1. **Manual Testing Checklist:**
   - [ ] Command registers properly
   - [ ] Item throwing works
   - [ ] Seed cracking succeeds
   - [ ] RNG maintenance works for all events
   - [ ] Config saves/loads correctly
   - [ ] Modded server detection works
   - [ ] Infinite tools feature works
   - [ ] Tool break warning displays
   - [ ] Works in both singleplayer and multiplayer

2. **Automated Testing:**
   - Create test world
   - Execute command programmatically
   - Verify RNG state changes
   - Test edge cases (no items, wrong server, etc.)

### Phase 9: Documentation

#### 9.1 User Documentation
- Installation instructions
- Usage guide
- Configuration options
- Troubleshooting
- FAQ

#### 9.2 Developer Documentation
- API documentation
- Event system
- Extension points
- Version compatibility notes

### Phase 10: Distribution

#### 10.1 Build Configuration
```groovy
jar {
    manifest {
        attributes([
            "Specification-Title": "Player RNG Cracker",
            "Specification-Vendor": "YourName",
            "Specification-Version": "1",
            "Implementation-Title": project.name,
            "Implementation-Version": project.version,
            "Implementation-Vendor": "YourName",
            "Implementation-Timestamp": new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
}
```

#### 10.2 Publishing
- CurseForge
- Modrinth
- GitHub Releases

## Key Challenges and Solutions

### Challenge 1: Mixin Limitations in Forge
**Problem:** Forge has less comprehensive Mixin support than Fabric.
**Solution:** 
- Use Forge events where possible
- Use Access Transformers for field/method access
- Only use Mixins for truly unavoidable cases
- Consider reflection as last resort

### Challenge 2: Event System Differences
**Problem:** Fabric's event system is more flexible for custom events.
**Solution:**
- Create custom Forge events extending `Event`
- Use `MinecraftForge.EVENT_BUS.post()` for firing
- Document event contracts clearly

### Challenge 3: Client Commands
**Problem:** Forge handles client commands differently.
**Solution:**
- Use `RegisterClientCommandsEvent` (requires Forge 40.1.0+)
- For older versions, register as normal command with client-side check
- Validate execution context carefully

### Challenge 4: Configuration Persistence
**Problem:** Forge config is TOML-based and structured differently.
**Solution:**
- Map Fabric config to Forge ConfigSpec
- Handle enum serialization carefully
- Implement config change listeners for reactive updates

### Challenge 5: Library Compatibility
**Problem:** LattiCG and other dependencies may have conflicts.
**Solution:**
- Use Shadow plugin to relocate packages
- Test with common Forge mods for conflicts
- Document known incompatibilities

## Performance Considerations

### Optimization Strategies
1. **Event Handler Efficiency**
   - Only register events when RNG is cracked
   - Use early returns to minimize processing
   - Cache frequently accessed config values

2. **Task Management**
   - Limit concurrent tasks
   - Use throttling for item throws
   - Implement task priority system

3. **Memory Management**
   - Clear completed tasks promptly
   - Limit history buffer size
   - Use weak references where appropriate

## Compatibility Matrix

| Minecraft Version | Forge Version | Status | Notes |
|-------------------|---------------|--------|-------|
| 1.20.1 | 47.x | ✓ Supported | Primary target |
| 1.19.4 | 45.x | ✓ Supported | Minor API differences |
| 1.19.2 | 43.x | ✓ Supported | Older event system |
| 1.18.2 | 40.x | ⚠ Partial | Client commands limited |
| 1.16.5 | 36.x | ✗ Not supported | Significant API changes |

## Migration Checklist

### Pre-Development
- [ ] Set up Forge development environment
- [ ] Configure build.gradle with dependencies
- [ ] Set up Access Transformers
- [ ] Configure Mixin plugin if needed

### Core Features
- [ ] Port command registration
- [ ] Port CCrackRng.java
- [ ] Port PlayerRandCracker.java
- [ ] Port CCrackRngGen.java (copy directly)
- [ ] Port ServerBrandManager.java
- [ ] Implement task management system

### Event System
- [ ] Map all Fabric events to Forge equivalents
- [ ] Create custom PlayerRNGCallEvent
- [ ] Implement event handlers
- [ ] Test event firing and handling

### Configuration
- [ ] Create ForgeConfigSpec
- [ ] Implement config GUI (optional)
- [ ] Test config persistence
- [ ] Add config change listeners

### UI/UX
- [ ] Port translation files
- [ ] Implement feedback messages
- [ ] Add overlay messages
- [ ] Test chat formatting

### Advanced Features
- [ ] Implement infinite tools
- [ ] Add tool break warnings
- [ ] Port chorus manipulation
- [ ] Test all RNG manipulation features

### Testing
- [ ] Unit tests for RNG implementation
- [ ] Integration tests for command
- [ ] Manual testing in singleplayer
- [ ] Manual testing in multiplayer
- [ ] Test with modded servers

### Documentation
- [ ] Write user guide
- [ ] Create configuration docs
- [ ] Document API for developers
- [ ] Write troubleshooting guide

### Release
- [ ] Build final JAR
- [ ] Test on clean instance
- [ ] Prepare release notes
- [ ] Publish to distribution platforms

## Estimated Effort

| Phase | Estimated Time | Complexity |
|-------|----------------|------------|
| Project Setup | 2-4 hours | Low |
| Core Components | 4-6 hours | Medium |
| Event System | 6-8 hours | High |
| Mixin/Hooks | 8-12 hours | High |
| Algorithm Port | 2-3 hours | Low |
| Network Handling | 3-5 hours | Medium |
| Task Management | 4-6 hours | Medium |
| Testing | 8-10 hours | Medium |
| Documentation | 4-6 hours | Low |
| Distribution | 2-3 hours | Low |
| **Total** | **43-63 hours** | **Medium-High** |

## Conclusion

Porting the crackPlayerRNG feature from Fabric to Forge is a significant undertaking that requires:

1. **Deep understanding** of both Fabric and Forge architectures
2. **Careful event system migration** with custom events where needed
3. **Strategic use of Mixins** where Forge hooks are insufficient
4. **Robust testing** to ensure RNG synchronization
5. **Clear documentation** for users and developers

The core algorithm (CCrackRngGen) ports directly, but the surrounding infrastructure requires substantial rework to fit Forge's patterns. Success depends on thorough testing and attention to the nuances of each platform's event system and lifecycle management.
