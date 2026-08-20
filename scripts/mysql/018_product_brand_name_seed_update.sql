USE mall_lite;

UPDATE pms_product
SET brand_name = CASE
                     WHEN product_code = 'SEED-P-001' THEN 'Seed Brand'
                     WHEN product_code = 'FRONT-P-001' THEN 'LiteHome'
                     WHEN product_code = 'FRONT-P-002' THEN 'CeramiLife'
                     WHEN product_code = 'FRONT-P-003' THEN 'JoyCeramic'
                     WHEN product_code = 'FRONT-P-004' THEN 'HeatGuard'
                     WHEN product_code = 'FRONT-P-005' THEN 'ClosetPlus'
                     WHEN product_code = 'FRONT-P-006' THEN 'WoodNatur'
                     WHEN product_code = 'FRONT-P-007' THEN 'SteelMate'
                     WHEN product_code = 'FRONT-P-008' THEN 'LinenBelle'
                     WHEN product_code = 'FRONT-P-009' THEN 'RattanHome'
                     WHEN product_code = 'FRONT-P-010' THEN 'KitchenPro'
                     WHEN product_code = 'FRONT-P-011' THEN 'WoodTray'
                     WHEN product_code = 'FRONT-P-012' THEN 'MiniChef'
                     WHEN product_code = 'FRONT-P-013' THEN 'CleanBot'
                     WHEN product_code = 'FRONT-P-014' THEN 'SoftPaper'
                     WHEN product_code = 'FRONT-P-015' THEN 'SoundLite'
                     WHEN product_code = 'FRONT-P-016' THEN 'CottonNest'
                     WHEN product_code = 'FRONT-P-017' THEN 'FreshCare'
                     WHEN product_code = 'FRONT-P-018' THEN 'ClearBox'
                     WHEN product_code = 'FRONT-P-019' THEN 'SpiceTurn'
                     WHEN product_code = 'FRONT-P-020' THEN 'NordicMug'
                     WHEN product_code = 'FRONT-P-021' THEN 'KnitHome'
                     WHEN product_code = 'FRONT-P-022' THEN 'AromaStone'
                     WHEN product_code = 'FRONT-P-023' THEN 'ChargeMate'
                     WHEN product_code = 'FRONT-P-024' THEN 'DrawerMax'
                     WHEN product_code = 'FRONT-P-025' THEN 'GlassCool'
                     WHEN product_code = 'FRONT-P-026' THEN 'CottonHotel'
                     WHEN product_code = 'FRONT-P-027' THEN 'FabricCare'
                     WHEN product_code = 'FRONT-P-028' THEN 'MistMini'
                     WHEN product_code = 'FRONT-P-029' THEN 'DoorSpace'
                     WHEN product_code = 'FRONT-P-030' THEN 'SiliconeChef'
                     WHEN product_code = 'FRONT-P-031' THEN 'LinenTable'
                     WHEN product_code = 'FRONT-P-032' THEN 'OilClean'
                     WHEN product_code = 'FRONT-P-033' THEN 'NightGlow'
                     WHEN product_code = 'FRONT-P-034' THEN 'FoldBasket'
                     WHEN product_code = 'FRONT-P-035' THEN 'FruitKnife'
                     WHEN product_code = 'FRONT-P-036' THEN 'CurtainEase'
                     WHEN product_code = 'FRONT-P-037' THEN 'TravelKit'
                     WHEN product_code = 'FRONT-P-038' THEN 'KeyMaster'
                     WHEN product_code LIKE 'IPHONE%' OR product_code LIKE 'iPhone%' THEN 'Apple'
                     WHEN product_code LIKE 'DJI%' THEN 'DJI'
                     ELSE 'Mall Lite'
    END
WHERE deleted = 0
  AND (brand_name IS NULL OR brand_name = '');

UPDATE pms_product
SET brand_name = 'Apple'
WHERE deleted = 0
  AND (product_code LIKE 'IPHONE%' OR product_code LIKE 'iPhone%');

UPDATE pms_product
SET brand_name = 'DJI'
WHERE deleted = 0
  AND product_code LIKE 'DJI%';
