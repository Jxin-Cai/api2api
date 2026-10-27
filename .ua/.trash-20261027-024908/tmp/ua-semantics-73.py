from importlib.machinery import SourceFileLoader
m=SourceFileLoader('ua_writer_73','/Users/jxin/Desktop/Project/products/2api/api2api/.ua/tmp/ua-semantic-writer-8.py').load_module()
meta={'ScopedRateLimitTest.java':('验证带调用方范围错误码的上游 429 被归类为调用方限流，而普通 429 仍保留提供方限流隔离，避免耗尽调用方额度时误罚渠道。',['测试','限流分类','渠道隔离'],'调用方与提供方限流分类回归')}
overrides={'test_classifies_caller_scope_when_upstream_returns_scoped_429':'参数化验证并发、分组额度、用户及凭证限流错误码均被识别为调用方限流。'}
m.run(73,meta,overrides)
