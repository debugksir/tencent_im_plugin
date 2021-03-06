import 'package:tencent_im_plugin/entity/group_member_entity.dart';
import 'package:tencent_im_plugin/list_util.dart';

/// 群属性更新实体
class GroupAttributeChangedEntity {
  /// 群ID
  String groupID;

  /// 群成员列表
  Map<String, String> attributes;

  GroupAttributeChangedEntity.fromJson(Map<String, dynamic> json) {
    groupID = json['groupID'];
    attributes = (json["attributes"] as Map).cast<String, String>();
  }
}
