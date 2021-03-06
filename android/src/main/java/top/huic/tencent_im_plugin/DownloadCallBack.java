package top.huic.tencent_im_plugin;

import com.tencent.imsdk.v2.V2TIMDownloadCallback;
import com.tencent.imsdk.v2.V2TIMElem;

import java.util.HashMap;

import io.flutter.plugin.common.MethodChannel;
import top.huic.tencent_im_plugin.enums.ListenerTypeEnum;

/**
 * 下载回调
 */
public class DownloadCallBack implements V2TIMDownloadCallback {

    /**
     * 结果方法
     */
    private MethodChannel.Result result;

    /**
     * 下载路径
     */
    private String path;

    /**
     * 消息ID
     */
    private String msgId;

    public DownloadCallBack() {
    }

    public DownloadCallBack(String path) {
        this.path = path;
    }

    public DownloadCallBack(MethodChannel.Result result, String path, String msgId) {
        this.result = result;
        this.path = path;
        this.msgId = msgId;
    }

    @Override
    public void onProgress(final V2TIMElem.V2ProgressInfo v2ProgressInfo) {
        TencentImPlugin.invokeListener(ListenerTypeEnum.DownloadProgress, new HashMap<String, Object>() {
            {
                put("msgId", msgId);
                put("currentSize", v2ProgressInfo.getCurrentSize());
                put("totalSize", v2ProgressInfo.getTotalSize());
            }
        });
    }

    /**
     * 成功事件
     */
    @Override
    public void onSuccess() {
        if (result != null) {
            result.success(path);
        }
    }


    /**
     * 失败事件
     *
     * @param code 错误码
     * @param desc 错误描述
     */
    @Override
    public void onError(int code, String desc) {
        if (this.result != null) {
            result.error(String.valueOf(code), desc, desc);
        }
    }
}
