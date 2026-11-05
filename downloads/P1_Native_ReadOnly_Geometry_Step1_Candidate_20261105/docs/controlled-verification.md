# 未来本机静态审核与受控验证

本候选交付不执行以下真机步骤。只在相应明确授权下操作独立测试环境，不改变SISWSheetMetalConvert、Web、生产配置、服务脚本、模型或Ribbon持久化。

1. 校验外部ZIP.sha256与内部SHA256SUMS，核对本机otk.jar与manifest散列。需要Java25加载实际SDK，不能因插件class major61就换用Java17。
2. 使用独立新classes目录，不能覆盖或混入BatchRename类。薄JAR包含相同6个class、没有SDK或Main-Class，不用java -jar运行；优先按已验证的classes路径形式填写模板。registration中的路径占位符必须替换，text_dir指向本候选resources/text。
3. 先核对独立命令注册、消息文件解析及实际加载class身份。start不自动采集，不启动CREOSON或操作模型。若Designate失败先保留日志，不绕过异常伪报注册成功。不得为了触发命令写入持久化Ribbon；入口触发方式需在本机受控范围内明确。
4. 仅打开既有测试普通零件、核对初始模型身份/修改状态，再明确触发SISWReadOnlyGeometryCollect。观察UI是否返回，留存独立JSONL及stderr、class/SDK/资源/dat身份、前后模型和配置观察。当前SDK公开接口是否支持全部代理对象仍未知，异常应保留为证据。
5. 对默认体、所有返回体及真实面类型/面积做人工抽查；不要因为日志数量与HTTP222一致就认证所有值。原生IsSheetmetal结果与几何形态分开解释。未知体归属、无几何实体或quilt/复杂模型可能导致PARTIAL，不能为达成100%修改模型。
6. 若模型dirty/身份变化、运行卡住、禁止调用或异常状态出现，停止后续操作，保留记录；不自动Save、Undo、删除、再生或重试。单次SDK调用不可强行取消，按现场既有异常处置流程处理，不将超时说成可安全中断。卸载时若采集中则不强制卸载，解除失败保留重启要求。
7. 不点击转换，不保存模型、不新增浏览器桥、不修改Web控件。stop只解除自己的监听器。配置恢复按本机原有授权和证据单独处理，本包不执行恢复。

建议首轮先验证无模型/非零件可读阻断和一个已知普通测试件的读取；没有单独授权时仅静态审核，不通过制造模型写失败测试覆盖。第二步采样应另立候选，不把本包升级成等厚识别验收。

可复核编译命令为Java25 javac --release 17 -encoding UTF-8 -proc:none -Xlint:all -classpath <真实otk.jar> -d <新classes目录> source/GeometryEvidenceSupport.java source/P1ReadOnlyGeometryCollector.java。本轮已由云端真实SDK编译完成，不要求本机重复编译才能审阅；本机的重点是身份审核和真实环境验证。
