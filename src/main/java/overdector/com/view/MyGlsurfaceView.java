package overdector.com.view;

import android.content.Context;
import android.opengl.GLSurfaceView;

/**
 * Created by mozzie on 17/9/1.
 */

public class MyGlsurfaceView extends GLSurfaceView{
    /**
     * Standard View constructor. In order to render something, you
     * must call {@link #setRenderer} to register a renderer.
     *
     * @param context
     */
    public MyGlsurfaceView(Context context) {
        super(context);
    }
}
