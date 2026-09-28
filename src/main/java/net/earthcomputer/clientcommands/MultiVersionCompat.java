package net.earthcomputer.clientcommands;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

// Simplified for Forge 1.20.1 - only RNG features, no ViaVersion support needed
public class MultiVersionCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final int V1_13_2 = 404;
    public static final int V1_17 = 755;
    public static final int V1_18 = 757;
    public static final int V1_20 = 763;

    public static final MultiVersionCompat INSTANCE = new MultiVersionCompat();

    public int getProtocolVersion() {
        return SharedConstants.getProtocolVersion();
    }

    public String getProtocolName() {
        return SharedConstants.getCurrentVersion().getName();
    }

    public boolean doesItemExist(Item item) {
        return true;
    }
}
