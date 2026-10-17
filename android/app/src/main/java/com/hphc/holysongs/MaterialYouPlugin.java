package com.hphc.holysongs;

import android.content.res.Resources;
import android.os.Build;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Exposes Android 12+ (API 31) Material You wallpaper-derived system colors
 * to the web layer, so the app can adopt dynamic color accents.
 */
@CapacitorPlugin(name = "MaterialYou")
public class MaterialYouPlugin extends Plugin {

    @PluginMethod
    public void getColors(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("available", false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Resources res = getContext().getResources();
                String accent1 = resolve(res, "system_accent1_400");
                String accent1Deep = resolve(res, "system_accent1_600");
                String accent2 = resolve(res, "system_accent2_500");
                String neutral = resolve(res, "system_neutral1_700");
                ret.put("available", true);
                ret.put("accent1", accent1);
                ret.put("accent1Deep", accent1Deep);
                ret.put("accent2", accent2);
                ret.put("neutral", neutral);
            } catch (Exception ignored) {
                // Some OEM builds hide the system color resources; stay unavailable.
            }
        }
        call.resolve(ret);
    }

    private String resolve(Resources res, String name) {
        int id = res.getIdentifier(name, "color", "android");
        if (id == 0) {
            throw new IllegalArgumentException("missing color: " + name);
        }
        int color = res.getColor(id, getContext().getTheme());
        return String.format("#%06X", 0xFFFFFF & color);
    }
}
