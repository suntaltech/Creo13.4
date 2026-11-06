# 解析参数来源与语义边界

真实SDK来源是用户提供Creo13.4 otk.jar，SHA256=0f95b0cf9c88792c0bd18aff4e7a3ef730fdc3d7924ffe681779a40bfcda2319。以Java25 javap核对实际公共签名，随后用该JAR编译。接口存在/编译通过不等于云端已执行该接口或核实所有官方几何约定。

| 证据 | 真实公共API | 处理 |
|---|---|---|
| 解析描述符 | Surface.GetSurfaceDescriptor | null/异常保留，拒绝臆造替代 |
| 类型/取向 | SurfaceDescriptor.GetSurfaceType/GetOrientation | 与本轮原生面类型核对，保留取向原代码 |
| 平面描述 | PlaneDescriptor，继承TransformedSurfaceDescriptor | 只经公共instanceof分支，不用内部反射 |
| 圆柱描述/半径 | CylinderDescriptor.GetRadius | 必须有限正值，模型长度单位 |
| 坐标系 | TransformedSurfaceDescriptor.GetCoordSys | 读取Transform3D.GetOrigin/GetXAxis/GetYAxis/GetZAxis |
| 候选点参数反求 | Surface.EvalParameters(Point3D) | 参数仍须原生VerifyUV，反求本身不证明域内 |
| 3D复核 | Surface.Eval3DData，SurfXYZData.GetParams/GetPoint/GetNormal | 返回UV精确、双INSIDE、有限法向/XYZ回读 |

不调用GetNURBSRepresentation、EvalClosestPoint、SetRadius、SetCoordSys、SetOrigin、Invert或任何模型写方法。Point3D.create/set只构造每次调用的新参数对象；UVParams等原参数对象用法不变。

GetCoordSys的原始原点/轴记录为模型上下文中SDK返回值，Z轴作为法向/轴线候选，未冒称已重新查阅完整官方坐标语义。先检查有限/非零/轴正交，再检查三个真实本轮样本：平面点到候选支撑平面的残差、法向平行；圆柱点到候选轴的径向长度与原生半径一致、原生法向与径向平行且垂轴。原始SDK取向、候选轴符号及法向符号关系都保留，不翻转为“实体外向”。

数值长度护栏是512×ulp(max(1,有关坐标/半径的绝对值))，角向护栏4096×ulp(1.0)用于轴点积/叉积与残差检查；它们是显式浮点表示策略，不从相对精度0.0012推导，不代替未知绝对模型公差，不改变任何INSIDE分类。近乎平行轴采用叉积幅值判断，避免1-cos的近零抵消。几何不一致/数值超限返回UNKNOWN，不能通过扩大护栏或改模型精度凑成功。

平行支撑平面给出法向偏移的绝对值候选；圆柱仅在轴候选数值平行、轴间垂向偏移在上述护栏内时给出半径差绝对值候选。不平行平面无固定支撑间隙，非同轴圆柱不输出半径差当间隙。即使给出支撑间隙，也不是修剪后最近距离、实壁厚度或材料两侧认证。

参数失败/单位未知/描述符公共类型不符/取向未知均记录源头，不从包围盒或旧日志反推解析半径/坐标轴。已有Step6采样/粗配对结果先保留，新解析阶段在其后才运行。历史指标参考仍仅诊断，不用作新参数或新原生点。
