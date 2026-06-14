# 后台商品与秒杀管理模块设计

## 一、目标

新增一个正式的后台管理域，支持管理员维护全站商品、SKU、库存、秒杀活动和秒杀商品。当前系统没有店铺维度，因此第一版所有商品都归平台统一管理，不引入店铺、商家、管理员账号和权限校验。

后台页面仍放在现有 `mall-frontend` 工程中，不单独启动新的前端端口；后端新增独立 `mall-admin` Maven 模块承载后台管理编排逻辑，由 `mall-app` 作为启动模块暴露 `/api/admin/**` 接口。

## 二、范围

第一版包含：

1. 商品 SPU 增删改查、上下架和分页筛选。
2. SKU 增删改查、状态维护和库存维护。
3. 秒杀活动增删改查、启用禁用和手动预热。
4. 秒杀活动商品选择、编辑、移除和重新预热。
5. 前端后台布局、商品管理页面和秒杀管理页面。

第一版不包含：

1. 管理员登录、角色、权限和菜单授权。
2. 店铺、商家、品牌和分类管理。
3. 商品审核、操作日志、富文本图片上传。
4. 秒杀自动审批、复杂防刷策略和独立后台前端工程。

## 三、总体架构

后端新增 `mall-admin` 模块，专门承载后台管理用例。`mall-admin` 依赖已有 `mall-product`、`mall-inventory`、`mall-seckill`、`mall-common` 等模块，复用已有实体、Mapper 和基础服务能力。

`mall-app` 引入 `mall-admin`，并在 `mall-app` 中放置后台 Controller，或者由 `mall-admin` 提供 Service、DTO、VO，`mall-app` 负责 Web 层装配。为保持与当前项目控制器位置一致，第一版推荐 Controller 仍放在 `mall-app`，后台业务编排放在 `mall-admin`。

前端仍使用现有 Vite 工程和端口，新增后台路由分组：

```text
/admin
/admin/products
/admin/products/new
/admin/products/:id
/admin/seckill
/admin/seckill/new
/admin/seckill/:id
```

前端代码独立放在 `src/views/admin` 和 `src/api/admin`，后续如需拆成独立后台工程，可以按目录迁移。

## 四、商品管理设计

后台商品管理以商品 SPU 为主表单，SKU 和库存作为商品下的明细。

后端新增 `AdminProductService`，提供：

1. 商品列表：支持分页、分类、商品名称、商品编码和状态筛选。后台查询默认包含上下架商品，但排除已软删除商品。
2. 商品详情：返回商品基础信息、SKU 列表和每个 SKU 的库存信息。
3. 新增商品：事务内创建 `pms_product`、`pms_sku` 和 `ims_inventory`，避免商品、SKU、库存出现半完成状态。
4. 编辑商品：更新商品基础信息；SKU 支持新增、修改和软删除；库存按 SKU 同步更新。
5. 删除商品：软删除商品和所属 SKU，前台查询自然不可见，并保留历史订单、购物车、秒杀记录引用安全。
6. 上下架：单独提供状态切换接口，便于列表页快速操作。

前端页面：

1. `/admin/products`：商品列表页，包含筛选区、表格、分页、新增、编辑、删除、上架和下架操作。
2. `/admin/products/new`：商品新增页。
3. `/admin/products/:id`：商品编辑页，上半部分维护 SPU，下半部分维护 SKU 明细和库存数量。

关键校验：

1. 商品编码和 SKU 编码必填且唯一。
2. 商品至少保留一个未删除 SKU。
3. 价格、库存和排序不能为负。
4. 编辑 SKU 时，后端必须校验 SKU 属于当前商品。
5. 删除使用软删除，不物理删除历史数据。

## 五、秒杀管理设计

秒杀管理复用现有 `sms_seckill_activity` 和 `sms_seckill_sku` 表，后台服务集中放在 `AdminSeckillService`。

后端提供：

1. 秒杀活动列表：支持分页、活动名称、状态和时间范围筛选。后台可查看未开始、进行中、已结束和禁用活动。
2. 秒杀活动详情：返回活动基础信息和已选择的活动 SKU 列表，包含商品名、SKU 名、原价、秒杀价、活动库存、限购数量、排序和状态。
3. 新增和编辑活动：维护名称、开始时间、结束时间、状态和备注。
4. 选择秒杀商品：从普通商品 SKU 池中选择 SKU，填写秒杀价、活动库存、每人限购、排序和状态，写入 `sms_seckill_sku`。
5. 编辑和移除秒杀商品：编辑秒杀价、库存、限购、排序和状态；移除时软删除。
6. 活动预热：复用现有 `SeckillService.preheatActivity`，后台页面提供手动预热按钮。

对正在进行或即将开始的活动，编辑活动商品后允许手动预热；第一版不强制自动预热，避免后台保存操作对 Redis 库存产生难以察觉的副作用。

前端页面：

1. `/admin/seckill`：活动列表页，支持筛选、新增、编辑、启用、禁用、删除和预热。
2. `/admin/seckill/new`：新增活动页。
3. `/admin/seckill/:id`：活动编辑页，上半部分维护活动信息，下半部分维护活动 SKU。选择商品时弹窗展示可选 SKU 列表，支持按商品名和 SKU 编码筛选。

关键校验：

1. 活动结束时间必须晚于开始时间。
2. 秒杀价必须大于 0，且不能高于 SKU 原价。
3. 秒杀库存必须大于等于 0。
4. 每人限购必须大于 0，且不能超过秒杀库存。
5. 同一活动内同一 SKU 不能重复选择。
6. 秒杀商品必须引用未删除的商品和 SKU。

## 六、接口草案

商品后台接口：

```text
GET    /api/admin/products
POST   /api/admin/products
GET    /api/admin/products/{productId}
PUT    /api/admin/products/{productId}
DELETE /api/admin/products/{productId}
PUT    /api/admin/products/{productId}/status
GET    /api/admin/products/skus
```

秒杀后台接口：

```text
GET    /api/admin/seckill/activities
POST   /api/admin/seckill/activities
GET    /api/admin/seckill/activities/{activityId}
PUT    /api/admin/seckill/activities/{activityId}
DELETE /api/admin/seckill/activities/{activityId}
PUT    /api/admin/seckill/activities/{activityId}/status
POST   /api/admin/seckill/activities/{activityId}/preheat
POST   /api/admin/seckill/activities/{activityId}/skus
PUT    /api/admin/seckill/activities/{activityId}/skus/{seckillSkuId}
DELETE /api/admin/seckill/activities/{activityId}/skus/{seckillSkuId}
```

`GET /api/admin/products/skus` 用于秒杀选择商品弹窗，返回可选 SKU 池，包含商品名、SKU 名、商品编码、SKU 编码、原价、状态和库存。

## 七、数据一致性

新增和编辑商品时，商品、SKU 和库存必须在同一个事务内完成。SKU 删除采用软删除，库存记录随 SKU 变更为不可用或软删除状态，避免前台继续售卖。

秒杀活动商品使用现有唯一索引 `uk_activity_sku(activity_id, sku_id)` 保证同一活动 SKU 不重复。后台删除活动商品时软删除；如果唯一索引会阻止重新选择已软删除 SKU，实施阶段需要通过恢复软删除记录或调整唯一约束策略解决，优先选择恢复软删除记录。

后台保存商品后需要清理商品详情缓存和热门商品详情缓存，避免前台看到旧数据。若已有缓存服务没有删除接口，实施时补充失效能力。

## 八、测试设计

后端重点覆盖：

1. 商品新增同时创建 SKU 和库存。
2. 商品编辑可以新增、更新、软删除 SKU，并同步库存。
3. 删除商品后前台商品查询不可见。
4. 商品编码和 SKU 编码唯一校验。
5. 秒杀活动时间校验。
6. 秒杀商品不能重复选择。
7. 秒杀价不能高于 SKU 原价。
8. 秒杀商品删除后活动详情不再返回。
9. 手动预热接口仍能把活动库存写入 Redis。

前端重点覆盖构建验证和人工页面验收：

1. 后台路由可以进入。
2. 商品列表、新增、编辑、删除主流程可操作。
3. 秒杀活动列表、新增、编辑、选择商品和预热主流程可操作。
4. 表单错误有明确提示，列表空数据和接口失败有可见状态。

## 九、实施顺序

1. 新增 `mall-admin` 模块和依赖装配。
2. 商品后台 DTO、VO、Service、Controller 和测试。
3. 秒杀后台 DTO、VO、Service、Controller 和测试。
4. 前端后台布局、API 封装和商品页面。
5. 前端秒杀页面和选择 SKU 弹窗。
6. 运行后端聚焦测试、前端构建，并用浏览器人工验证主要页面。
