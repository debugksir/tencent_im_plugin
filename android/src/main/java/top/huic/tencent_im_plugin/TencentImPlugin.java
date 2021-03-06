package top.huic.tencent_im_plugin;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tencent.imsdk.v2.V2TIMConversation;
import com.tencent.imsdk.v2.V2TIMConversationResult;
import com.tencent.imsdk.v2.V2TIMCreateGroupMemberInfo;
import com.tencent.imsdk.v2.V2TIMFriendAddApplication;
import com.tencent.imsdk.v2.V2TIMFriendApplication;
import com.tencent.imsdk.v2.V2TIMFriendApplicationResult;
import com.tencent.imsdk.v2.V2TIMFriendCheckResult;
import com.tencent.imsdk.v2.V2TIMFriendGroup;
import com.tencent.imsdk.v2.V2TIMFriendInfo;
import com.tencent.imsdk.v2.V2TIMFriendInfoResult;
import com.tencent.imsdk.v2.V2TIMFriendOperationResult;
import com.tencent.imsdk.v2.V2TIMGroupApplication;
import com.tencent.imsdk.v2.V2TIMGroupApplicationResult;
import com.tencent.imsdk.v2.V2TIMGroupInfo;
import com.tencent.imsdk.v2.V2TIMGroupInfoResult;
import com.tencent.imsdk.v2.V2TIMGroupMemberFullInfo;
import com.tencent.imsdk.v2.V2TIMGroupMemberInfoResult;
import com.tencent.imsdk.v2.V2TIMGroupMemberOperationResult;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMMessage;
import com.tencent.imsdk.v2.V2TIMOfflinePushConfig;
import com.tencent.imsdk.v2.V2TIMOfflinePushInfo;
import com.tencent.imsdk.v2.V2TIMSDKConfig;
import com.tencent.imsdk.v2.V2TIMSendCallback;
import com.tencent.imsdk.v2.V2TIMSignalingInfo;
import com.tencent.imsdk.v2.V2TIMUserFullInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import top.huic.tencent_im_plugin.entity.CustomConversationEntity;
import top.huic.tencent_im_plugin.entity.CustomConversationResultEntity;
import top.huic.tencent_im_plugin.entity.CustomMessageEntity;
import top.huic.tencent_im_plugin.entity.FindMessageEntity;
import top.huic.tencent_im_plugin.enums.ListenerTypeEnum;
import top.huic.tencent_im_plugin.enums.MessageNodeType;
import top.huic.tencent_im_plugin.listener.CustomAdvancedMsgListener;
import top.huic.tencent_im_plugin.listener.CustomConversationListener;
import top.huic.tencent_im_plugin.listener.CustomFriendshipListener;
import top.huic.tencent_im_plugin.listener.CustomGroupListener;
import top.huic.tencent_im_plugin.listener.CustomSDKListener;
import top.huic.tencent_im_plugin.listener.CustomSignalingListener;
import top.huic.tencent_im_plugin.message.AbstractMessageNode;
import top.huic.tencent_im_plugin.message.entity.AbstractMessageEntity;
import top.huic.tencent_im_plugin.util.CommonUtil;
import top.huic.tencent_im_plugin.util.JsonUtil;
import top.huic.tencent_im_plugin.util.TencentImUtils;

/**
 * TencentImPlugin
 */
public class TencentImPlugin implements FlutterPlugin, MethodCallHandler {
    /**
     * ???????????????
     */
    public static Context context;

    /**
     * ???Flutter???????????????
     */
    private static MethodChannel channel;

    public TencentImPlugin() {
    }

    private TencentImPlugin(Context context, MethodChannel channel) {
        TencentImPlugin.context = context;
        TencentImPlugin.channel = channel;
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.mask;
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "tencent_im_plugin");
        channel.setMethodCallHandler(new TencentImPlugin(flutterPluginBinding.getApplicationContext(), channel));
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "tencent_im_plugin");
        channel.setMethodCallHandler(new TencentImPlugin(registrar.context(), channel));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        try {
            Method method = this.getClass().getDeclaredMethod(call.method, MethodCall.class, Result.class);
            method.setAccessible(true);
            method.invoke(this, call, result);
        } catch (NoSuchMethodException e) {
            result.notImplemented();
        } catch (IllegalAccessException e) {
            result.notImplemented();
        } catch (InvocationTargetException ignored) {
        }
    }


    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
    }

    /**
     * ?????????Im???????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void initSDK(MethodCall methodCall, Result result) {
        String appid = CommonUtil.getParam(methodCall, result, "appid");
        Integer logPrintLevel = methodCall.argument("logPrintLevel");

        // ????????? SDK
        V2TIMSDKConfig sdkConfig = new V2TIMSDKConfig();
        if (logPrintLevel != null) {
            sdkConfig.setLogLevel(logPrintLevel);
        }
        V2TIMManager.getInstance().initSDK(context, Integer.parseInt(appid), sdkConfig, new CustomSDKListener());

        // ??????????????????
        V2TIMManager.getMessageManager().addAdvancedMsgListener(new CustomAdvancedMsgListener());

        // ?????????????????????
        V2TIMManager.getConversationManager().setConversationListener(new CustomConversationListener());

        // ??????????????????
        V2TIMManager.getInstance().setGroupListener(new CustomGroupListener());

        // ????????????????????????
        V2TIMManager.getFriendshipManager().setFriendListener(new CustomFriendshipListener());

        // ?????????????????????
        V2TIMManager.getSignalingManager().addSignalingListener(new CustomSignalingListener());

        result.success(null);
    }


    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void unInitSDK(MethodCall methodCall, Result result) {
        V2TIMManager.getInstance().unInitSDK();
        result.success(null);
    }

    /**
     * ????????? IM ??????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void login(MethodCall methodCall, final Result result) {
        String userID = CommonUtil.getParam(methodCall, result, "userID");
        String userSig = CommonUtil.getParam(methodCall, result, "userSig");

        // ????????????
        V2TIMManager.getInstance().login(userID, userSig, new VoidCallBack(result));
    }

    /**
     * ????????? IM ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void logout(MethodCall methodCall, final Result result) {
        V2TIMManager.getInstance().logout(new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getLoginStatus(MethodCall methodCall, final Result result) {
        result.success(V2TIMManager.getInstance().getLoginStatus());
    }

    /**
     * ????????? ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getLoginUser(MethodCall methodCall, final Result result) {
        result.success(V2TIMManager.getInstance().getLoginUser());
    }

    /**
     * ???????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void invite(MethodCall methodCall, final Result result) {
        String invitee = CommonUtil.getParam(methodCall, result, "invitee");
        String data = CommonUtil.getParam(methodCall, result, "data");
        Boolean onlineUserOnly = CommonUtil.getParam(methodCall, result, "onlineUserOnly");
        String offlinePushInfoStr = CommonUtil.getParam(methodCall, result, "offlinePushInfo");
        int timeout = CommonUtil.getParam(methodCall, result, "timeout");

        // ??????????????????????????????JSON??????????????????????????????
        JSONObject jsonObject = JSON.parseObject(offlinePushInfoStr);
        V2TIMOfflinePushInfo offlinePushInfo = JSON.parseObject(offlinePushInfoStr, V2TIMOfflinePushInfo.class);
        if (jsonObject.get("disablePush") != null) {
            offlinePushInfo.disablePush(jsonObject.getBoolean("disablePush"));
        }

        // ????????????????????????????????????
        result.success(V2TIMManager.getSignalingManager().invite(invitee, data, onlineUserOnly, offlinePushInfo, timeout, new VoidCallBack(null)));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void inviteInGroup(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        List<String> inviteeList = Arrays.asList(CommonUtil.getParam(methodCall, result, "inviteeList").toString().split(","));
        String data = CommonUtil.getParam(methodCall, result, "data");
        Boolean onlineUserOnly = CommonUtil.getParam(methodCall, result, "onlineUserOnly");
        int timeout = CommonUtil.getParam(methodCall, result, "timeout");

        // ????????????????????????????????????
        result.success(V2TIMManager.getSignalingManager().inviteInGroup(groupID, inviteeList, data, onlineUserOnly, timeout, new VoidCallBack(null)));
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void cancel(MethodCall methodCall, final Result result) {
        String inviteID = CommonUtil.getParam(methodCall, result, "inviteID");
        String data = CommonUtil.getParam(methodCall, result, "data");
        V2TIMManager.getSignalingManager().cancel(inviteID, data, new VoidCallBack(result));
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void accept(MethodCall methodCall, final Result result) {
        String inviteID = CommonUtil.getParam(methodCall, result, "inviteID");
        String data = CommonUtil.getParam(methodCall, result, "data");
        V2TIMManager.getSignalingManager().accept(inviteID, data, new VoidCallBack(result));
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void reject(MethodCall methodCall, final Result result) {
        String inviteID = CommonUtil.getParam(methodCall, result, "inviteID");
        String data = CommonUtil.getParam(methodCall, result, "data");
        V2TIMManager.getSignalingManager().reject(inviteID, data, new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getSignalingInfo(MethodCall methodCall, final Result result) {
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(message, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                result.success(JsonUtil.toJSONString(V2TIMManager.getSignalingManager().getSignalingInfo(message)));
            }
        });
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void addInvitedSignaling(MethodCall methodCall, final Result result) {
        String info = CommonUtil.getParam(methodCall, result, "info");
        V2TIMManager.getSignalingManager().addInvitedSignaling(JSON.parseObject(info, V2TIMSignalingInfo.class), new VoidCallBack(result));
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void sendMessage(MethodCall methodCall, final Result result) {
        String receiver = methodCall.argument("receiver");
        final String groupID = methodCall.argument("groupID");
        String nodeStr = CommonUtil.getParam(methodCall, result, "node");
        Map node = JSON.parseObject(nodeStr, Map.class);
        boolean ol = CommonUtil.getParam(methodCall, result, "ol");
        Integer localCustomInt = methodCall.argument("localCustomInt");
        String localCustomStr = methodCall.argument("localCustomStr");
        Integer priority = CommonUtil.getParam(methodCall, result, "priority");
        String offlinePushInfo = methodCall.argument("offlinePushInfo");

        // ????????????????????????V2TIMMessage??????
        AbstractMessageNode messageNode = MessageNodeType.getMessageNodeTypeByV2TIMConstant(Integer.valueOf(node.get("nodeType").toString())).getMessageNodeInterface();
        AbstractMessageEntity messageEntity = (AbstractMessageEntity) JSON.parseObject(nodeStr, messageNode.getEntityClass());
        V2TIMMessage message = messageNode.getV2TIMMessage(messageEntity);
        if (localCustomInt != null) {
            message.setLocalCustomInt(localCustomInt);
        }
        if (localCustomStr != null) {
            message.setLocalCustomData(localCustomStr);
        }

        final String[] msgId = new String[1];
        V2TIMSendCallback<V2TIMMessage> callback = new V2TIMSendCallback<V2TIMMessage>() {
            @Override
            public void onError(final int i, final String s) {
                TencentImPlugin.invokeListener(ListenerTypeEnum.MessageSendFail, new HashMap<String, Object>() {
                    {
                        put("msgId", msgId);
                        put("code", i);
                        put("desc", s);
                    }
                });
            }

            @Override
            public void onSuccess(V2TIMMessage o) {
                TencentImPlugin.invokeListener(ListenerTypeEnum.MessageSendSucc, new CustomMessageEntity(o));
            }

            @Override
            public void onProgress(final int i) {
                TencentImPlugin.invokeListener(ListenerTypeEnum.MessageSendProgress, new HashMap<String, Object>() {
                    {
                        put("msgId", msgId);
                        put("progress", i);
                    }
                });
            }
        };

        // ????????????
        msgId[0] = V2TIMManager.getMessageManager().sendMessage(message, receiver, groupID, priority, ol, offlinePushInfo == null ? null : JSON.parseObject(offlinePushInfo, V2TIMOfflinePushInfo.class), callback);
        result.success(msgId[0]);
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void revokeMessage(MethodCall methodCall, final Result result) {
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(message, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                V2TIMManager.getMessageManager().revokeMessage(message, new VoidCallBack(result));
            }
        });
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getC2CHistoryMessageList(MethodCall methodCall, final Result result) {
        final String userID = CommonUtil.getParam(methodCall, result, "userID");
        final int count = CommonUtil.getParam(methodCall, result, "count");
        final String lastMsgStr = methodCall.argument("lastMsg");

        // ??????????????????
        final ValueCallBack<List<V2TIMMessage>> resultCallBack = new ValueCallBack<List<V2TIMMessage>>(result) {
            @Override
            public void onSuccess(List<V2TIMMessage> v2TIMMessages) {
                List<CustomMessageEntity> resultData = new ArrayList<>(v2TIMMessages.size());
                for (V2TIMMessage v2TIMMessage : v2TIMMessages) {
                    resultData.add(new CustomMessageEntity(v2TIMMessage));
                }
                result.success(JsonUtil.toJSONString(resultData));
            }
        };


        // ??????????????????????????????????????????????????????
        if (lastMsgStr == null) {
            V2TIMManager.getMessageManager().getC2CHistoryMessageList(userID, count, null, resultCallBack);
        } else {
            TencentImUtils.getMessageByFindMessageEntity(lastMsgStr, new ValueCallBack<V2TIMMessage>(result) {
                @Override
                public void onSuccess(V2TIMMessage message) {
                    V2TIMManager.getMessageManager().getC2CHistoryMessageList(userID, count, message, resultCallBack);
                }
            });
        }
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupHistoryMessageList(MethodCall methodCall, final Result result) {
        final String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        final int count = CommonUtil.getParam(methodCall, result, "count");
        String lastMsgStr = methodCall.argument("lastMsg");

        // ??????????????????
        final ValueCallBack<List<V2TIMMessage>> resultCallBack = new ValueCallBack<List<V2TIMMessage>>(result) {
            @Override
            public void onSuccess(List<V2TIMMessage> v2TIMMessages) {
                List<CustomMessageEntity> resultData = new ArrayList<>(v2TIMMessages.size());
                for (V2TIMMessage v2TIMMessage : v2TIMMessages) {
                    resultData.add(new CustomMessageEntity(v2TIMMessage));
                }
                result.success(JsonUtil.toJSONString(resultData));
            }
        };


        // ??????????????????????????????????????????????????????
        if (lastMsgStr == null) {
            V2TIMManager.getMessageManager().getGroupHistoryMessageList(groupID, count, null, resultCallBack);
        } else {
            TencentImUtils.getMessageByFindMessageEntity(lastMsgStr, new ValueCallBack<V2TIMMessage>(result) {
                @Override
                public void onSuccess(V2TIMMessage message) {
                    V2TIMManager.getMessageManager().getGroupHistoryMessageList(groupID, count, message, resultCallBack);
                }
            });
        }
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void markC2CMessageAsRead(MethodCall methodCall, final Result result) {
        String userID = CommonUtil.getParam(methodCall, result, "userID");
        V2TIMManager.getMessageManager().markC2CMessageAsRead(userID, new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void markGroupMessageAsRead(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        V2TIMManager.getMessageManager().markGroupMessageAsRead(groupID, new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteMessageFromLocalStorage(MethodCall methodCall, final Result result) {
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(message, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                V2TIMManager.getMessageManager().deleteMessageFromLocalStorage(message, new VoidCallBack(result));
            }
        });
    }

    /**
     * ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteMessages(MethodCall methodCall, final Result result) {
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(JSON.parseArray(message, FindMessageEntity.class), new ValueCallBack<List<V2TIMMessage>>(result) {
            @Override
            public void onSuccess(List<V2TIMMessage> ms) {
                V2TIMManager.getMessageManager().deleteMessages(ms, new VoidCallBack(result));
            }
        });


    }

    /**
     * ??????????????????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void insertGroupMessageToLocalStorage(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String sender = CommonUtil.getParam(methodCall, result, "sender");
        String nodeStr = CommonUtil.getParam(methodCall, result, "node");
        Map node = JSON.parseObject(nodeStr, Map.class);

        // ??????????????????
        AbstractMessageNode messageNode = MessageNodeType.getMessageNodeTypeByV2TIMConstant(Integer.valueOf(node.get("nodeType").toString())).getMessageNodeInterface();
        AbstractMessageEntity messageEntity = (AbstractMessageEntity) JSON.parseObject(nodeStr, messageNode.getEntityClass());

        // ????????????
        V2TIMManager.getMessageManager().insertGroupMessageToLocalStorage(messageNode.getV2TIMMessage(messageEntity), groupID, sender, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                result.success(null);
            }
        });
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void downloadVideo(MethodCall methodCall, final Result result) {
        final String path = CommonUtil.getParam(methodCall, result, "path");
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(message, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                message.getVideoElem().downloadVideo(path, new DownloadCallBack(result, path, message.getMsgID()));
            }
        });
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void downloadVideoThumbnail(MethodCall methodCall, final Result result) {
        final String path = CommonUtil.getParam(methodCall, result, "path");
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(message, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                message.getVideoElem().downloadSnapshot(path, new DownloadCallBack(result, path, message.getMsgID()));
            }
        });
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void downloadSound(MethodCall methodCall, final Result result) {
        final String path = CommonUtil.getParam(methodCall, result, "path");
        String message = CommonUtil.getParam(methodCall, result, "message");
        TencentImUtils.getMessageByFindMessageEntity(message, new ValueCallBack<V2TIMMessage>(result) {
            @Override
            public void onSuccess(V2TIMMessage message) {
                message.getSoundElem().downloadSound(path, new DownloadCallBack(result, path, message.getMsgID()));
            }
        });
    }

    /**
     * ?????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void createGroup(MethodCall methodCall, final Result result) {
        String info = CommonUtil.getParam(methodCall, result, "info");
        String memberListStr = methodCall.argument("memberList");
        V2TIMManager.getGroupManager().createGroup(JSON.parseObject(info, V2TIMGroupInfo.class), memberListStr == null ? null : JSON.parseArray(memberListStr, V2TIMCreateGroupMemberInfo.class), new ValueCallBack<String>(result));
    }

    /**
     * ?????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void joinGroup(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String message = CommonUtil.getParam(methodCall, result, "message");
        V2TIMManager.getInstance().joinGroup(groupID, message, new VoidCallBack(result));
    }

    /**
     * ?????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void quitGroup(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        V2TIMManager.getInstance().quitGroup(groupID, new VoidCallBack(result));
    }

    /**
     * ?????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void dismissGroup(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        V2TIMManager.getInstance().dismissGroup(groupID, new VoidCallBack(result));
    }

    /**
     * ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getJoinedGroupList(MethodCall methodCall, final Result result) {
        V2TIMManager.getGroupManager().getJoinedGroupList(new ValueCallBack<List<V2TIMGroupInfo>>(result));
    }

    /**
     * ???????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupsInfo(MethodCall methodCall, final Result result) {
        String groupIDList = CommonUtil.getParam(methodCall, result, "groupIDList");
        V2TIMManager.getGroupManager().getGroupsInfo(Arrays.asList(groupIDList.split(",")), new ValueCallBack<List<V2TIMGroupInfoResult>>(result));
    }

    /**
     * ???????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setGroupInfo(MethodCall methodCall, final Result result) {
        String info = CommonUtil.getParam(methodCall, result, "info");
        V2TIMManager.getGroupManager().setGroupInfo(JSON.parseObject(info, V2TIMGroupInfo.class), new VoidCallBack(result));
    }

    /**
     * ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setReceiveMessageOpt(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        int opt = CommonUtil.getParam(methodCall, result, "opt");
        V2TIMManager.getGroupManager().setReceiveMessageOpt(groupID, opt, new VoidCallBack(result));
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void initGroupAttributes(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String attributes = CommonUtil.getParam(methodCall, result, "attributes");
        V2TIMManager.getGroupManager().initGroupAttributes(groupID, JSON.parseObject(attributes, HashMap.class), new VoidCallBack(result));
    }

    /**
     * ???????????????????????????????????????????????? value ?????????????????????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setGroupAttributes(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String attributes = CommonUtil.getParam(methodCall, result, "attributes");
        V2TIMManager.getGroupManager().setGroupAttributes(groupID, JSON.parseObject(attributes, HashMap.class), new VoidCallBack(result));
    }

    /**
     * ????????????????????????keys ??? null ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteGroupAttributes(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String keys = methodCall.argument("keys");
        V2TIMManager.getGroupManager().deleteGroupAttributes(groupID, keys == null ? null : Arrays.asList(keys.split(",")), new VoidCallBack(result));
    }

    /**
     * ????????????????????????keys ??? null ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupAttributes(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String keys = methodCall.argument("keys");
        V2TIMManager.getGroupManager().getGroupAttributes(groupID, keys == null ? null : Arrays.asList(keys.split(",")), new ValueCallBack<Map<String, String>>(result));
    }

    /**
     * ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupOnlineMemberCount(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        V2TIMManager.getGroupManager().getGroupOnlineMemberCount(groupID, new ValueCallBack<Integer>(result));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupMemberList(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        int filter = CommonUtil.getParam(methodCall, result, "filter");
        int nextSeq = CommonUtil.getParam(methodCall, result, "nextSeq");
        V2TIMManager.getGroupManager().getGroupMemberList(groupID, filter, nextSeq, new ValueCallBack<V2TIMGroupMemberInfoResult>(result));
    }

    /**
     * ?????????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupMembersInfo(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String memberList = CommonUtil.getParam(methodCall, result, "memberList");
        V2TIMManager.getGroupManager().getGroupMembersInfo(groupID, Arrays.asList(memberList.split(",")), new ValueCallBack<List<V2TIMGroupMemberFullInfo>>(result));
    }

    /**
     * ?????????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setGroupMemberInfo(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String info = CommonUtil.getParam(methodCall, result, "info");
        V2TIMManager.getGroupManager().setGroupMemberInfo(groupID, JSON.parseObject(info, V2TIMGroupMemberFullInfo.class), new VoidCallBack(result));
    }

    /**
     * ?????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void muteGroupMember(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String userID = CommonUtil.getParam(methodCall, result, "userID");
        int seconds = CommonUtil.getParam(methodCall, result, "seconds");
        V2TIMManager.getGroupManager().muteGroupMember(groupID, userID, seconds, new VoidCallBack(result));
    }


    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void inviteUserToGroup(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String userList = CommonUtil.getParam(methodCall, result, "userList");
        V2TIMManager.getGroupManager().inviteUserToGroup(groupID, Arrays.asList(userList.split(",")), new ValueCallBack<List<V2TIMGroupMemberOperationResult>>(result));
    }

    /**
     * ?????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void kickGroupMember(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String memberList = CommonUtil.getParam(methodCall, result, "memberList");
        String reason = CommonUtil.getParam(methodCall, result, "reason");
        V2TIMManager.getGroupManager().kickGroupMember(groupID, Arrays.asList(memberList.split(",")), reason, new ValueCallBack<List<V2TIMGroupMemberOperationResult>>(result));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setGroupMemberRole(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String userID = CommonUtil.getParam(methodCall, result, "userID");
        int role = CommonUtil.getParam(methodCall, result, "role");
        V2TIMManager.getGroupManager().setGroupMemberRole(groupID, userID, role, new VoidCallBack(result));
    }

    /**
     * ???????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void transferGroupOwner(MethodCall methodCall, final Result result) {
        String groupID = CommonUtil.getParam(methodCall, result, "groupID");
        String userID = CommonUtil.getParam(methodCall, result, "userID");
        V2TIMManager.getGroupManager().transferGroupOwner(groupID, userID, new VoidCallBack(result));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getGroupApplicationList(MethodCall methodCall, final Result result) {
        V2TIMManager.getGroupManager().getGroupApplicationList(new ValueCallBack<V2TIMGroupApplicationResult>(result));
    }


    /**
     * ??????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void acceptGroupApplication(MethodCall methodCall, final Result result) {
        String application = CommonUtil.getParam(methodCall, result, "application");
        final String reason = CommonUtil.getParam(methodCall, result, "reason");
        TencentImUtils.getGroupApplicationByFindGroupApplicationEntity(application, new ValueCallBack<V2TIMGroupApplication>(result) {
            @Override
            public void onSuccess(V2TIMGroupApplication v2TIMGroupApplication) {
                V2TIMManager.getGroupManager().acceptGroupApplication(v2TIMGroupApplication, reason, new VoidCallBack(result));
            }
        });
    }


    /**
     * ??????????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void refuseGroupApplication(MethodCall methodCall, final Result result) {
        final String application = CommonUtil.getParam(methodCall, result, "application");
        final String reason = CommonUtil.getParam(methodCall, result, "reason");
        TencentImUtils.getGroupApplicationByFindGroupApplicationEntity(application, new ValueCallBack<V2TIMGroupApplication>(result) {
            @Override
            public void onSuccess(V2TIMGroupApplication v2TIMGroupApplication) {
                V2TIMManager.getGroupManager().refuseGroupApplication(v2TIMGroupApplication, reason, new VoidCallBack(result));
            }
        });
    }

    /**
     * ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setGroupApplicationRead(MethodCall methodCall, final Result result) {
        V2TIMManager.getGroupManager().setGroupApplicationRead(new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getConversationList(MethodCall methodCall, final Result result) {
        int nextSeq = CommonUtil.getParam(methodCall, result, "nextSeq");
        int count = CommonUtil.getParam(methodCall, result, "count");
        V2TIMManager.getConversationManager().getConversationList(nextSeq, count, new ValueCallBack<V2TIMConversationResult>(result) {
            @Override
            public void onSuccess(V2TIMConversationResult v2TIMConversationResult) {
                result.success(JsonUtil.toJSONString(new CustomConversationResultEntity(v2TIMConversationResult)));
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getConversation(MethodCall methodCall, final Result result) {
        String conversationID = CommonUtil.getParam(methodCall, result, "conversationID");
        V2TIMManager.getConversationManager().getConversation(conversationID, new ValueCallBack<V2TIMConversation>(result) {
            @Override
            public void onSuccess(V2TIMConversation v2TIMConversation) {
                result.success(JsonUtil.toJSONString(new CustomConversationEntity(v2TIMConversation)));
            }
        });
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteConversation(MethodCall methodCall, final Result result) {
        String conversationID = CommonUtil.getParam(methodCall, result, "conversationID");
        V2TIMManager.getConversationManager().deleteConversation(conversationID, new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setConversationDraft(MethodCall methodCall, final Result result) {
        String conversationID = CommonUtil.getParam(methodCall, result, "conversationID");
        String draftText = methodCall.argument("draftText");
        V2TIMManager.getConversationManager().setConversationDraft(conversationID, draftText, new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getUsersInfo(MethodCall methodCall, final Result result) {
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getInstance().getUsersInfo(Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMUserFullInfo>>(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setSelfInfo(MethodCall methodCall, final Result result) {
        String info = CommonUtil.getParam(methodCall, result, "info");
        V2TIMManager.getInstance().setSelfInfo(JSON.parseObject(info, V2TIMUserFullInfo.class), new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void addToBlackList(MethodCall methodCall, final Result result) {
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getFriendshipManager().addToBlackList(Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMFriendOperationResult>>(result));
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteFromBlackList(MethodCall methodCall, final Result result) {
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getFriendshipManager().deleteFromBlackList(Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMFriendOperationResult>>(result));
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getBlackList(MethodCall methodCall, final Result result) {
        V2TIMManager.getFriendshipManager().getBlackList(new ValueCallBack<List<V2TIMFriendInfo>>(result));
    }

    /**
     * ????????? ??????????????????Token
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setOfflinePushConfig(MethodCall methodCall, final Result result) {
        String token = CommonUtil.getParam(methodCall, result, "token");
        Long bussid = Long.parseLong(CommonUtil.getParam(methodCall, result, "bussid").toString());
        V2TIMManager.getOfflinePushManager().setOfflinePushConfig(new V2TIMOfflinePushConfig(bussid, token), new VoidCallBack(result));
    }


    /**
     * ????????? ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setUnreadBadge(MethodCall methodCall, final Result result) {
        int number = CommonUtil.getParam(methodCall, result, "number");
        V2TIMManager.getOfflinePushManager().doBackground(number, new VoidCallBack(result));
    }

    /**
     * ????????? ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getFriendList(MethodCall methodCall, final Result result) {
        V2TIMManager.getFriendshipManager().getFriendList(new ValueCallBack<List<V2TIMFriendInfo>>(result));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getFriendsInfo(MethodCall methodCall, final Result result) {
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getFriendshipManager().getFriendsInfo(Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMFriendInfoResult>>(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setFriendInfo(MethodCall methodCall, final Result result) {
        String info = CommonUtil.getParam(methodCall, result, "info");
        V2TIMManager.getFriendshipManager().setFriendInfo(JSON.parseObject(info, V2TIMFriendInfo.class), new VoidCallBack(result));
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void addFriend(MethodCall methodCall, final Result result) {
        String info = CommonUtil.getParam(methodCall, result, "info");
        V2TIMManager.getFriendshipManager().addFriend(JSON.parseObject(info, V2TIMFriendAddApplication.class), new ValueCallBack<V2TIMFriendOperationResult>(result));
    }

    /**
     * ????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteFromFriendList(MethodCall methodCall, final Result result) {
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        int deleteType = CommonUtil.getParam(methodCall, result, "deleteType");
        V2TIMManager.getFriendshipManager().deleteFromFriendList(Arrays.asList(userIDList.split(",")), deleteType, new ValueCallBack<List<V2TIMFriendOperationResult>>(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void checkFriend(MethodCall methodCall, final Result result) {
        String userID = CommonUtil.getParam(methodCall, result, "userID");
        int checkType = CommonUtil.getParam(methodCall, result, "checkType");
        V2TIMManager.getFriendshipManager().checkFriend(userID, checkType, new ValueCallBack<V2TIMFriendCheckResult>(result));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getFriendApplicationList(MethodCall methodCall, final Result result) {
        V2TIMManager.getFriendshipManager().getFriendApplicationList(new ValueCallBack<V2TIMFriendApplicationResult>(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void acceptFriendApplication(MethodCall methodCall, final Result result) {
        final String application = CommonUtil.getParam(methodCall, result, "application");
        final int responseType = CommonUtil.getParam(methodCall, result, "responseType");
        TencentImUtils.getFriendApplicationByFindGroupApplicationEntity(application, new ValueCallBack<V2TIMFriendApplication>(result) {
            @Override
            public void onSuccess(V2TIMFriendApplication v2TIMFriendApplication) {
                V2TIMManager.getFriendshipManager().acceptFriendApplication(v2TIMFriendApplication, responseType, new ValueCallBack<V2TIMFriendOperationResult>(result));
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void refuseFriendApplication(MethodCall methodCall, final Result result) {
        String application = CommonUtil.getParam(methodCall, result, "application");
        TencentImUtils.getFriendApplicationByFindGroupApplicationEntity(application, new ValueCallBack<V2TIMFriendApplication>(result) {
            @Override
            public void onSuccess(V2TIMFriendApplication v2TIMFriendApplication) {
                V2TIMManager.getFriendshipManager().refuseFriendApplication(v2TIMFriendApplication, new ValueCallBack<V2TIMFriendOperationResult>(result));
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteFriendApplication(MethodCall methodCall, final Result result) {
        String application = CommonUtil.getParam(methodCall, result, "application");
        TencentImUtils.getFriendApplicationByFindGroupApplicationEntity(application, new ValueCallBack<V2TIMFriendApplication>(result) {
            @Override
            public void onSuccess(V2TIMFriendApplication v2TIMFriendApplication) {
                V2TIMManager.getFriendshipManager().deleteFriendApplication(v2TIMFriendApplication, new VoidCallBack(result));
            }
        });
    }

    /**
     * ???????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void setFriendApplicationRead(MethodCall methodCall, final Result result) {
        V2TIMManager.getFriendshipManager().setFriendApplicationRead(new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void createFriendGroup(MethodCall methodCall, final Result result) {
        String groupName = CommonUtil.getParam(methodCall, result, "groupName");
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getFriendshipManager().createFriendGroup(groupName, Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMFriendOperationResult>>(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void getFriendGroups(MethodCall methodCall, final Result result) {
        String groupNameList = methodCall.argument("groupNameList");
        V2TIMManager.getFriendshipManager().getFriendGroups(groupNameList == null ? null : Arrays.asList(groupNameList.split(",")), new ValueCallBack<List<V2TIMFriendGroup>>(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteFriendGroup(MethodCall methodCall, final Result result) {
        String groupNameList = CommonUtil.getParam(methodCall, result, "groupNameList");
        V2TIMManager.getFriendshipManager().deleteFriendGroup(Arrays.asList(groupNameList.split(",")), new VoidCallBack(result));
    }

    /**
     * ??????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void renameFriendGroup(MethodCall methodCall, final Result result) {
        String oldName = CommonUtil.getParam(methodCall, result, "oldName");
        String newName = CommonUtil.getParam(methodCall, result, "newName");
        V2TIMManager.getFriendshipManager().renameFriendGroup(oldName, newName, new VoidCallBack(result));
    }

    /**
     * ?????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void addFriendsToFriendGroup(MethodCall methodCall, final Result result) {
        String groupName = CommonUtil.getParam(methodCall, result, "groupName");
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getFriendshipManager().addFriendsToFriendGroup(groupName, Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMFriendOperationResult>>(result));
    }

    /**
     * ????????????????????????
     *
     * @param methodCall ??????????????????
     * @param result     ??????????????????
     */
    private void deleteFriendsFromFriendGroup(MethodCall methodCall, final Result result) {
        String groupName = CommonUtil.getParam(methodCall, result, "groupName");
        String userIDList = CommonUtil.getParam(methodCall, result, "userIDList");
        V2TIMManager.getFriendshipManager().deleteFriendsFromFriendGroup(groupName, Arrays.asList(userIDList.split(",")), new ValueCallBack<List<V2TIMFriendOperationResult>>(result));
    }

    /**
     * ???????????????
     *
     * @param type   ??????
     * @param params ??????
     */
    public static void invokeListener(ListenerTypeEnum type, Object params) {
        Map<String, Object> resultParams = new HashMap<>(2, 1);
        resultParams.put("type", type);
        resultParams.put("params", params == null ? null : JsonUtil.toJSONString(params));
        channel.invokeMethod("onListener", JsonUtil.toJSONString(resultParams));
    }
}