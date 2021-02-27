package com.heao.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

/**
 * 继承于此的子Fragment均会注册Broadcast Receiver
 * 此处Receiver被注册意味着应用在运行状态 应取消显示notification
 */
public abstract class VisibleFragment extends Fragment {
    private static final String TAG = "VisibleFragment";
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "canceling notification");
            // 设置返回值后 每个后续的接受者都可接收和修改此值
            setResultCode(Activity.RESULT_CANCELED);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // 自定义动作
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        // 注册 Dynamic Receiver
        // 使用自定义权限 只有包含此权限的intent可以触发该Receiver
        getActivity().registerReceiver(mOnShowNotification, filter,
                PollService.PERMISSION_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        // 注销Receiver
        getActivity().unregisterReceiver(mOnShowNotification);
    }


}
