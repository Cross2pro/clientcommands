import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

public class JarRemapper {
    // obf -> official mappings
    static Map<String, String> classMap = new HashMap<>();
    static Map<String, String> reverseClassMap = new HashMap<>(); // official -> obf
    static Map<String, String> methodMap = new HashMap<>(); // obfClass.obfMethod(obfDesc) -> officialMethod
    static Map<String, String> fieldMap = new HashMap<>(); // obfClass.obfField -> officialField
    
    public static void main(String[] args) throws Exception {
        String mappingsFile = args[0];
        String inputJar = args[1];
        String outputJar = args[2];
        
        System.out.println("Loading mappings...");
        loadProGuardMappings(mappingsFile);
        System.out.println("Classes: " + classMap.size() + ", Methods: " + methodMap.size() + ", Fields: " + fieldMap.size());
        
        System.out.println("Remapping JAR...");
        remapJar(inputJar, outputJar);
        System.out.println("Done: " + outputJar);
    }
    
    static void loadProGuardMappings(String file) throws Exception {
        // Pass 1: Load class mappings only
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            if (!line.startsWith(" ")) {
                int arrow = line.indexOf(" -> ");
                int colon = line.indexOf(":");
                if (arrow > 0) {
                    String official = line.substring(0, arrow).trim().replace('.', '/');
                    String obf = line.substring(arrow + 4, colon).trim();
                    classMap.put(obf, official);
                    reverseClassMap.put(official, obf);
                }
            }
        }
        br.close();

        // Pass 2: Load method/field mappings
        br = new BufferedReader(new FileReader(file));
        String currentObfClass = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            if (!line.startsWith(" ")) {
                int arrow = line.indexOf(" -> ");
                int colon = line.indexOf(":");
                if (arrow > 0) {
                    String obf = line.substring(arrow + 4, colon).trim();
                    currentObfClass = obf;
                }
            } else {
                // Member mapping
                line = line.trim();
                int arrow = line.indexOf(" -> ");
                if (arrow > 0 && currentObfClass != null) {
                    String left = line.substring(0, arrow).trim();
                    String obfName = line.substring(arrow + 4).trim();
                    int paren = left.indexOf('(');
                    if (paren > 0) {
                        // Method
                        int space = left.lastIndexOf(' ', paren);
                        String methodName = left.substring(space + 1, paren);
                        int parenEnd = left.indexOf(')', paren);
                        String params = left.substring(paren + 1, parenEnd);
                        String returnType = left.substring(0, space).trim();
                        String[] tokens = returnType.split(" ");
                        String ret = tokens[tokens.length - 1];
                        // Strip line number prefix like "57:94:" from return type
                        int colonIdx = ret.lastIndexOf(':');
                        if (colonIdx >= 0 && ret.substring(0, colonIdx).matches("[0-9:]+")) {
                            ret = ret.substring(colonIdx + 1);
                        }
                        // Build obfuscated descriptor (what's actually in the class file)
                        String obfDesc = buildObfMethodDesc(params, ret);
                        methodMap.put(currentObfClass + "." + obfName + obfDesc, methodName);
                    } else {
                        // Field
                        int space = left.lastIndexOf(' ');
                        String fieldName = left.substring(space + 1);
                        fieldMap.put(currentObfClass + "." + obfName, fieldName);
                    }
                }
            }
        }
        br.close();
    }

    static String buildObfMethodDesc(String params, String ret) {
        StringBuilder sb = new StringBuilder("(");
        if (!params.trim().isEmpty()) {
            for (String p : params.split(",")) {
                sb.append(toObfDesc(p.trim()));
            }
        }
        sb.append(")").append(toObfDesc(ret));
        return sb.toString();
    }

    static String toObfDesc(String type) {
        switch (type) {
            case "void": return "V";
            case "boolean": return "Z";
            case "byte": return "B";
            case "char": return "C";
            case "short": return "S";
            case "int": return "I";
            case "long": return "J";
            case "float": return "F";
            case "double": return "D";
            default:
                if (type.endsWith("[]")) {
                    return "[" + toObfDesc(type.substring(0, type.length() - 2));
                }
                String internal = type.replace('.', '/');
                String obf = reverseClassMap.getOrDefault(internal, internal);
                return "L" + obf + ";";
        }
    }
    
    static String buildMethodDesc(String params, String ret) {
        StringBuilder sb = new StringBuilder("(");
        if (!params.trim().isEmpty()) {
            for (String p : params.split(",")) {
                sb.append(toDesc(p.trim()));
            }
        }
        sb.append(")").append(toDesc(ret));
        return sb.toString();
    }
    
    static String toDesc(String type) {
        switch (type) {
            case "void": return "V";
            case "boolean": return "Z";
            case "byte": return "B";
            case "char": return "C";
            case "short": return "S";
            case "int": return "I";
            case "long": return "J";
            case "float": return "F";
            case "double": return "D";
            default:
                if (type.endsWith("[]")) {
                    return "[" + toDesc(type.substring(0, type.length() - 2));
                }
                return "L" + type.replace('.', '/') + ";";
        }
    }
    
    static String remapDesc(String desc) {
        // Remap class names in descriptor from obf to official
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < desc.length()) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String cls = desc.substring(i + 1, end);
                String remapped = classMap.getOrDefault(cls, cls);
                sb.append('L').append(remapped).append(';');
                i = end + 1;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
    
    static String remapClass(String obf) {
        return classMap.getOrDefault(obf, obf);
    }
    
    static void remapJar(String input, String output) throws Exception {
        try (JarFile jar = new JarFile(input);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(output))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                try (InputStream is = jar.getInputStream(entry)) {
                    byte[] data = is.readAllBytes();
                    if (name.endsWith(".class")) {
                        data = remapClass(data);
                        // Rename the entry
                        String obfClass = name.substring(0, name.length() - 6);
                        String officialClass = remapClass(obfClass);
                        name = officialClass + ".class";
                    }
                    JarEntry newEntry = new JarEntry(name);
                    jos.putNextEntry(newEntry);
                    jos.write(data);
                    jos.closeEntry();
                }
            }
        }
    }
    
    static byte[] remapClass(byte[] classData) {
        ClassReader cr = new ClassReader(classData);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        Remapper remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                return remapClass(internalName);
            }
            @Override
            public String mapMethodName(String owner, String name, String desc) {
                if (name.equals("<init>") || name.equals("<clinit>")) return name;
                // methodMap is now keyed by obfuscated descriptor directly
                String result = methodMap.get(owner + "." + name + desc);
                return result != null ? result : name;
            }
            @Override
            public String mapFieldName(String owner, String name, String desc) {
                String result = fieldMap.get(owner + "." + name);
                return result != null ? result : name;
            }
        };
        ClassRemapper classRemapper = new ClassRemapper(cw, remapper);
        cr.accept(classRemapper, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
