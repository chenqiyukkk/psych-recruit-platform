UPDATE sys_config SET config_value = '注意力,认知控制,记忆,情绪,感知觉,决策,社会认知,语言,执行功能,学习,眼动,脑电EEG,核磁共振,问卷调查,行为实验' WHERE config_key = 'experiment_tags';
SELECT config_key, config_value FROM sys_config WHERE config_key = 'experiment_tags';
