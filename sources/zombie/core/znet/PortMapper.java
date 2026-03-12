/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import zombie.core.znet.PortMappingEntry;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;

public class PortMapper {
    private static String externalAddress;

    public static void startup() {
    }

    public static void shutdown() {
        PortMapper._cleanup();
    }

    public static boolean discover() {
        PortMapper._discover();
        return PortMapper._igd_found();
    }

    public static boolean igdFound() {
        return PortMapper._igd_found();
    }

    public static boolean addMapping(int wanPort, int lanPort, String description, String proto, int leaseTime) {
        return PortMapper.addMapping(wanPort, lanPort, description, proto, leaseTime, false);
    }

    public static boolean addMapping(int wanPort, int lanPort, String description, String proto, int leaseTime, boolean force) {
        boolean result = PortMapper._add_mapping(wanPort, lanPort, description, proto, leaseTime, force);
        if (!result && leaseTime != 0) {
            DebugLog.log(DebugType.Network, "Failed to add port mapping, retrying with zero lease time");
            result = PortMapper._add_mapping(wanPort, lanPort, description, proto, 0, force);
        }
        return result;
    }

    public static boolean removeMapping(int wanPort, String proto) {
        return PortMapper._remove_mapping(wanPort, proto);
    }

    public static void fetchMappings() {
        PortMapper._fetch_mappings();
    }

    public static int numMappings() {
        return PortMapper._num_mappings();
    }

    public static PortMappingEntry getMapping(int index) {
        return PortMapper._get_mapping(index);
    }

    public static String getGatewayInfo() {
        return PortMapper._get_gateway_info();
    }

    public static synchronized String getExternalAddress(boolean forceUpdate) {
        if (forceUpdate || externalAddress == null) {
            externalAddress = PortMapper._get_external_address();
        }
        return externalAddress;
    }

    public static String getExternalAddress() {
        return PortMapper.getExternalAddress(false);
    }

    private static native void _discover();

    private static native void _cleanup();

    private static native boolean _igd_found();

    private static native boolean _add_mapping(int var0, int var1, String var2, String var3, int var4, boolean var5);

    private static native boolean _remove_mapping(int var0, String var1);

    private static native void _fetch_mappings();

    private static native int _num_mappings();

    private static native PortMappingEntry _get_mapping(int var0);

    private static native String _get_gateway_info();

    private static native String _get_external_address();
}

