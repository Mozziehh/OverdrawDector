package overdector.com.permission;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by maolei on 16/10/24.
 */

public class MIUIUtils {

    private static final String SP_MIUI_KEY = "isMIUI";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";

    public static boolean isMIUI(Context context) {
        if (context == null) {
            return false;
        }
        return false;
    }

}
