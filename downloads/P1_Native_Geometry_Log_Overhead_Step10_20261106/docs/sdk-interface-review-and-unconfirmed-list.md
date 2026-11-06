# SDK只读可行性核对与未确认接口

来源为用户真实otk.jar，SHA256=0f95b0cf9c88792c0bd18aff4e7a3ef730fdc3d7924ffe681779a40bfcda2319。使用Java25 javap读取20个相关公共类型声明，并直接解析public类文件方法表，对1612个public-facing类、8076个public声明方法建立限定名称索引。排除Implementation包，没有加载/执行SDK类或JNI，不调用模型。

签名存在不证明语义、许可或运行时可调用，更不证明只读契约。本轮接口目录12项全部runtimeInvokedByThisStep=false。WModel/WSolid等是公共扩展接口，但未对当前对象做强制转换、兼容性/许可试探或调用。

| 能力线索 | 已见签名 | 仍未确认 |
|---|---|---|
| 射线相交 | WModel.ComputeRayIntersections(double, Ray) | double参数语义/单位；向量归一化；正反方向；排序、深度、切触/重复/自命中；修剪、体/装配路径、许可和只读性 |
| 射线参数对象 | wfcModel.Ray_Create(Point3D, Vector3D)；Ray的点/向量访问器 | 参数对象生存期和坐标约定；构造不提供命中/材料证据 |
| 点投影 | WSolid.ProjectPoint(Point3D, double)→ProjectionInfo | double含义；是否最近/支撑面/修剪语义；Surface/UV结果不是材料内外布尔值 |
| 点处几何 | WPart.FindGeometry(Point3D)→Selections | 近邻/容差/拓扑含义和体作用域；不等价点在实体内部 |
| 壳/空腔 | WSolid.FindShellsAndVoids()→ShellData | 壳拓扑与空间占据关系、封闭/非流形/空腔边界；不是点或线段材料判定 |
| 净距 | SelectionEvaluator.ComputeClearance() | 支持哪些SelectionPair、最近点/干涉含义、许可和只读性；不是壁厚 |
| 临界距离 | SelectionEvaluator.ComputeNearestCriticalDistance() | UV/点/距离的精确域与选择限制，不证明材料夹层或遮挡 |
| 干涉 | SelectionEvaluator.ComputeInterference(boolean) | boolean含义、返回几何对象生命周期/副作用、同体面的合法性；未确认前不调用 |
| 命中解释 | Selection.GetPoint/GetDepth/GetSelItem/GetSelModel/GetPath | GetDepth不能先假定已排序射线距离；坐标、体归属和路径待核对 |
| 材料属性 | WSolidBody.GetMaterial() | Material对象只是属性分配，不回答世界点/线段是否占据材料 |
| 直接材料占据 | 在已检查声明/限定名称模式中未确认直接point-in-solid契约 | 不是对所有SDK、其他语言或其他方法名的不存在性证明 |
| 直接遮挡 | 未确认直接线段遮挡契约 | 射线/距离候选不自动提供材料区间、最近遮挡和可靠入/出顺序 |

pfcInterference.CreateSelectionEvaluator(SelectionPair)签名存在，但本轮不构造Evaluator；GlobalEvaluator/ComputeGlobalInterference也未执行。Surface.EvalClosestPoint和VerifyUV等已有能力不能代替实体材料占据：前者是曲面距离线索，后者是曲面参数域分类。

未查阅并认证完整官方语义、未做现场对象/许可/结果排序试验。本轮不凭方法名承诺只读可行性，也不把“未确认”改成“不可用”。候选分类只能说明已有点距形态，不能验证上述API。

完整声明在evidence/sdk-readonly-interface-review.javap.txt，方法名索引与范围记录分别在sdk-public-method-name-index.json和sdk-interface-review-scope.json。声明文件会包含Create/Set等方法，那不是插件调用清单；实际插件58项方法引用看compiled-sdk-calls.json，与Step7完全相同，没有射线/干涉/材料/遮挡新增引用。

后续若另行定义受控验证任务，应先确认只读契约、参数/单位、封闭体与孔/切触/共面情况、初始自命中排除、多体路径和遮挡区间，再设计有界探针；本包没有自动开展这些新操作。
