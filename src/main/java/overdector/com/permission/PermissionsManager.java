package overdector.com.permission;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A class to help you manage your permissions simply.
 */
public class PermissionsManager {

    private static final String TAG = PermissionsManager.class.getSimpleName();
    private static final String WUBA_PACKAGE_URL = "package:com.wuba"; // 方案

    private final Set<String> mPendingRequests = new HashSet<String>(1);
    private final Set<String> mPermissions = new HashSet<String>(1);
    private final List<PermissionsResultAction> mPendingActions = new ArrayList<PermissionsResultAction>(1);

    private static PermissionsManager mInstance = null;
    public static final int PEMISSION_REQUEST_CODE = 7;
    private Context mContext;

    public static PermissionsManager getInstance() {
        if (mInstance == null) {
            mInstance = new PermissionsManager();
        }
        return mInstance;
    }

    private PermissionsManager() {
        initializePermissionsMap();
    }

    /**
     * This method uses reflection to read all the permissions in the Manifest class.
     * This is necessary because some permissions do not exist on older versions of Android,
     * since they do not exist, they will be denied when you check whether you have permission
     * which is problematic since a new permission is often added where there was no previous
     * permission required. We initialize a Set of available permissions and check the set
     * when checking if we have permission since we want to know when we are denied a permission
     * because it doesn't exist yet.
     */
    private synchronized void initializePermissionsMap() {
        Field[] fields = Manifest.permission.class.getFields();
        for (Field field : fields) {
            String name = null;
            try {
                name = (String) field.get("");
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Could not access field", e);
            }
            mPermissions.add(name);
        }
    }

    /**
     * This method retrieves all the permissions declared in the application's manifest.
     * It returns a non null array of permisions that can be declared.
     *
     * @param activity the Activity necessary to check what permissions we have.
     * @return a non null array of permissions that are declared in the application manifest.
     */
    @NonNull
    private synchronized String[] getManifestPermissions(@NonNull final Activity activity) {
        PackageInfo packageInfo = null;
        List<String> list = new ArrayList<String>(1);
        try {
            Log.d(TAG, activity.getPackageName());
            packageInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "A problem occurred when retrieving permissions", e);
        }
        if (packageInfo != null) {
            String[] permissions = packageInfo.requestedPermissions;
            if (permissions != null) {
                for (String perm : permissions) {
                    Log.d(TAG, "Manifest contained permission: " + perm);
                    list.add(perm);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * This method adds the {@link PermissionsResultAction} to the current list
     * of pending actions that will be completed when the permissions are
     * received. The list of permissions passed to this method are registered
     * in the PermissionsResultAction object so that it will be notified of changes
     * made to these permissions.
     *
     * @param permissions the required permissions for the action to be executed.
     * @param action      the action to add to the current list of pending actions.
     */
    private synchronized void addPendingAction(@NonNull String[] permissions,
                                               @Nullable PermissionsResultAction action) {
        if (action == null) {
            return;
        }
        action.registerPermissions(permissions);
        mPendingActions.add(action);
    }

    /**
     * This method removes a pending action from the list of pending actions.
     * It is used for cases where the permission has already been granted, so
     * you immediately wish to remove the pending action from the queue and
     * execute the action.
     *
     * @param action the action to remove
     */
    private synchronized void removePendingAction(@Nullable PermissionsResultAction action) {
        for (Iterator<PermissionsResultAction> iterator = mPendingActions.iterator();
             iterator.hasNext(); ) {
            PermissionsResultAction weakRef = iterator.next();
            if (weakRef == action || weakRef == null) {
                iterator.remove();
            }
        }
    }

    /**
     * This static method can be used to check whether or not you have a specific permission.
     * It is basically a less verbose method of using {@link ActivityCompat#checkSelfPermission(Context, String)}
     * and will simply return a boolean whether or not you have the permission. If you pass
     * in a null Context object, it will return false as otherwise it cannot check the permission.
     * However, the Activity parameter is nullable so that you can pass in a reference that you
     * are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context    the Context necessary to check the permission
     * @param permission the permission to check
     * @return true if you have been granted the permission, false otherwise
     */
    public boolean hasPermission(@Nullable Context context, @NonNull String permission) {
        return context != null && (checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED );
    }

    /**
     * This static method can be used to check whether or not you have several specific permissions.
     * It is simpler than checking using {@link ActivityCompat#checkSelfPermission(Context, String)}
     * for each permission and will simply return a boolean whether or not you have all the permissions.
     * If you pass in a null Context object, it will return false as otherwise it cannot check the
     * permission. However, the Activity parameter is nullable so that you can pass in a reference
     * that you are not always sure will be valid or not (e.g. getActivity() from Fragment).
     *
     * @param context     the Context necessary to check the permission
     * @param permissions the permissions to check
     * @return true if you have been granted all the permissions, false otherwise
     */
    public synchronized boolean hasAllPermissions(@Nullable Context context, @NonNull String[] permissions) {
        if (context == null) {
            return false;
        }
        boolean hasAllPermissions = true;
        for (String perm : permissions) {
            hasAllPermissions &= hasPermission(context, perm);
        }
        return hasAllPermissions;
    }

    /**
     * This method will request all the permissions declared in your application manifest
     * for the specified {@link PermissionsResultAction}. The purpose of this method is to enable
     * all permissions to be requested at one shot. The PermissionsResultAction is used to notify
     * you of the user allowing or denying each permission. The Activity and PermissionsResultAction
     * parameters are both annotated Nullable, but this method will not work if the Activity
     * is null. It is only annotated Nullable as a courtesy to prevent crashes in the case
     * that you call this from a Fragment where {@link Fragment#getActivity()} could yield
     * null. Additionally, you will not receive any notification of permissions being granted
     * if you provide a null PermissionsResultAction.
     *
     * @param activity the Activity necessary to request and check permissions.
     * @param action   the PermissionsResultAction used to notify you of permissions being accepted.
     */
    public synchronized void requestAllManifestPermissionsIfNecessary(final @Nullable Activity activity,
                                                                      final @Nullable PermissionsResultAction action) {
        if (activity == null) {
            return;
        }
        String[] perms = getManifestPermissions(activity);
        requestPermissionsIfNecessaryForResult(activity, perms, action);
    }

    /**
     * This method should be used to execute a {@link PermissionsResultAction} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Activity variable is nullable, but if it is null, the method will fail to execute.
     * This is only nullable as a courtesy for Fragments where getActivity() may yeild null
     * if the Fragment is not currently added to its parent Activity.
     *
     * @param activity    the activity necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link PermissionsResultAction}.
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     */
    public synchronized void requestPermissionsIfNecessaryForResult(@Nullable Activity activity,
                                                                    @NonNull String[] permissions,
                                                                    @Nullable PermissionsResultAction action) {
        if (activity == null) {
            return;
        }
        this.mPendingActions.clear();
        this.mPendingRequests.clear();
        addPendingAction(permissions, action);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            doPermissionWorkBeforeAndroidM(activity, permissions, action);
        } else {
            List<String> permList = getPermissionsListToRequest(activity, permissions, action);
            if (permList.isEmpty()) {
                //if there is no permission to request, there is no reason to keep the action int the list
                removePendingAction(action);
            } else {
                String[] permsToRequest = permList.toArray(new String[permList.size()]);
                mPendingRequests.addAll(permList);
                ActivityCompat.requestPermissions(activity, permsToRequest, 1);
            }
        }
    }

    /**
     * This method should be used to execute a {@link PermissionsResultAction} for the array
     * of permissions passed to this method. This method will request the permissions if
     * they need to be requested (i.e. we don't have permission yet) and will add the
     * PermissionsResultAction to the queue to be notified of permissions being granted or
     * denied. In the case of pre-Android Marshmallow, permissions will be granted immediately.
     * The Fragment variable is used, but if {@link Fragment#getActivity()} returns null, this method
     * will fail to work as the activity reference is necessary to check for permissions.
     *
     * @param fragment    the fragment necessary to request the permissions.
     * @param permissions the list of permissions to request for the {@link PermissionsResultAction}.
     * @param action      the PermissionsResultAction to notify when the permissions are granted or denied.
     */
    public synchronized void requestPermissionsIfNecessaryForResult(@NonNull Fragment fragment,
                                                                    @NonNull String[] permissions,
                                                                    @Nullable PermissionsResultAction action) {
        Activity activity = fragment.getActivity();
        if (activity == null) {
            return;
        }
        this.mPendingActions.clear();
        this.mPendingRequests.clear();
        addPendingAction(permissions, action);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            doPermissionWorkBeforeAndroidM(activity, permissions, action);
        } else {
            List<String> permList = getPermissionsListToRequest(activity, permissions, action);
            if (permList.isEmpty()) {
                //if there is no permission to request, there is no reason to keep the action int the list
                removePendingAction(action);
            } else {
                String[] permsToRequest = permList.toArray(new String[permList.size()]);
                mPendingRequests.addAll(permList);
                fragment.requestPermissions(permsToRequest, 1);
            }
        }
    }

    /**
     * This method notifies the PermissionsManager that the permissions have change. If you are making
     * the permissions requests using an Activity, then this method should be called from the
     * Activity callback onRequestPermissionsResult() with the variables passed to that method. If
     * you are passing a Fragment to make the permissions request, then you should call this in
     * the {@link Fragment#onRequestPermissionsResult(int, String[], int[])} method.
     * It will notify all the pending PermissionsResultAction objects currently
     * in the queue, and will remove the permissions request from the list of pending requests.
     *
     * @param permissions the permissions that have changed.
     * @param results     the values for each permission.
     */
    public synchronized void notifyPermissionsChange(Context context, @NonNull String[] permissions, @NonNull int[] results) {
        int size = permissions.length;
        if (results.length < size) {
            size = results.length;
        }
        Iterator<PermissionsResultAction> iterator = mPendingActions.iterator();
        while (iterator.hasNext()) {
            PermissionsResultAction action = iterator.next();
            // 保持原逻辑，在第一个拒绝后不在检查下一个权限，但是新增回调，通知权限情况
            boolean handled = false;
            for (int n = 0; n < size; n++) {
                if (action != null) {
                    action.onRequestPermissionsResult(1, permissions, results);
                    if (!handled) {
                        handled = checkMiPhoneResult(context, action, permissions[n], results[n]);
                    }
                }
            }
        }
        for (int n = 0; n < size; n++) {
            mPendingRequests.remove(permissions[n]);
        }
    }

    public synchronized void unregisterRequestAction(PermissionsResultAction action){
        if(action != null){
            mPendingActions.remove(action);
        }
    }

    private boolean isMIUIPhone(){
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    private boolean checkMiPhoneResult(Context context,PermissionsResultAction action,String permission ,int result){
        if(checkSelfPermission(context,permission) == PackageManager.PERMISSION_GRANTED){
            return action.onResult(permission,Permissions.GRANTED);
        }else {
            return action.onResult(permission,Permissions.DENIED);
        }

    }

    private List<String> PhonePermissions = Arrays.asList(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.USE_SIP,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.ADD_VOICEMAIL,
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG");

    private List<String> LocationPermissions = Arrays.asList(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION);

    /**
     * XiaoMi phone's permission Manager is special，try using AppOpsManager to judge whether its permission has been granted
     * this method is just for permission-group : phone & location.
     * for other phones and all sdk-ver < M ,use ActivityCompat.checkSelfPermission is Ok
     * @param context
     * @param permission
     * @return
     */
    private int checkSelfPermission(Context context, String permission){
        int permissionState = ActivityCompat.checkSelfPermission(context, permission);
        if(permissionState != PackageManager.PERMISSION_GRANTED){
            return permissionState;
        }

        if(!MIUIUtils.isMIUI(context)){
            return ActivityCompat.checkSelfPermission(context, permission);
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return ActivityCompat.checkSelfPermission(context, permission);
        }
        String op = "";
        //TODO ArrayList
        if(Manifest.permission.READ_PHONE_STATE.equals(permission)){
            op = AppOpsManager.OPSTR_READ_PHONE_STATE;
        }
        if(LocationPermissions.contains(permission)){
            op = AppOpsManager.OPSTR_FINE_LOCATION;
        }
        if(TextUtils.isEmpty(op)){
            return ActivityCompat.checkSelfPermission(context, permission);
        }
        try{
            AppOpsManager ops = context.getSystemService(AppOpsManager.class);
            int mode = ops.checkOp(op, Process.myUid(), context.getPackageName());
            Log.d(TAG,"mode = "  + mode);
            if (mode == AppOpsManager.MODE_ALLOWED) {
                //Accurate judgment for xiaomi
                return PackageManager.PERMISSION_GRANTED;
            } else {
                return PackageManager.PERMISSION_DENIED;
            }
        }catch (Exception e){
        }
        return ActivityCompat.checkSelfPermission(context, permission);

    }


    private static String getSystemProperty(String propName){
        String line;
        BufferedReader input = null;
        try
        {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        }
        catch (IOException ex)
        {
            Log.e(TAG, "Unable to read sysprop " + propName, ex);
            return null;
        }
        finally
        {
            if(input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Exception while closing InputStream", e);
                }
            }
        }
        return line;
    }
    /**
     * When request permissions on devices before Android M (Android 6.0, API Level 23)
     * Default granted
     *
     * @param activity    the activity to check permissions
     * @param permissions the permissions names
     * @param action      the callback work object, containing what we what to do after
     *                    permission check
     */
    private void doPermissionWorkBeforeAndroidM(@NonNull Activity activity,
                                                @NonNull String[] permissions,
                                                @Nullable PermissionsResultAction action) {
        for (String perm : permissions) {
            if (action != null) {
//                if (!mPermissions.contains(perm)) {
//                    action.onResult(perm, Permissions.NOT_FOUND);
//                } else if (ActivityCompat.checkSelfPermission(activity, perm)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    action.onResult(perm, Permissions.DENIED);
//                } else {
                    action.onResult(perm, Permissions.GRANTED);
//                }
            }
        }
    }

    /**
     * Filter the permissions list:
     * If a permission is not granted, add it to the result list
     * if a permission is granted, do the granted work, do not add it to the result list
     *
     * @param activity    the activity to check permissions
     * @param permissions all the permissions names
     * @param action      the callback work object, containing what we what to do after
     *                    permission check
     * @return a list of permissions names that are not granted yet
     */
    @NonNull
    private List<String> getPermissionsListToRequest(@NonNull Activity activity,
                                                     @NonNull String[] permissions,
                                                     @Nullable PermissionsResultAction action) {
        List<String> permList = new ArrayList<String>(permissions.length);
        for (String perm : permissions) {
            if (!mPermissions.contains(perm)) {
                if (action != null) {
                    action.onResult(perm, Permissions.NOT_FOUND);
                }
            } else if (checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                if (!mPendingRequests.contains(perm)) {
                    permList.add(perm);
                }
            } else {
                if (action != null) {
                    action.onResult(perm, Permissions.GRANTED);
                }
            }
        }
        return permList;
    }

    // 启动应用的设置
    public static void startAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(WUBA_PACKAGE_URL));
        activity.startActivityForResult(intent,PEMISSION_REQUEST_CODE);
    }

    // 启动应用的设置
    public static void startAppSettings(Fragment fragment) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(WUBA_PACKAGE_URL));
        fragment.startActivityForResult(intent,PEMISSION_REQUEST_CODE);
    }


}
