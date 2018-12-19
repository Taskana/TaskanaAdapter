SET SCHEMA TASKANA;

INSERT INTO HISTORY_EVENTS (BUSINESS_PROCESS_ID, PARENT_BUSINESS_PROCESS_ID, TASK_ID, EVENT_TYPE, CREATED, USER_ID, DOMAIN, WORKBASKET_KEY, POR_COMPANY, POR_SYSTEM, POR_INSTANCE, POR_TYPE, POR_VALUE, TASK_CLASSIFICATION_KEY, TASK_CLASSIFICATION_CATEGORY, ATTACHMENT_CLASSIFICATION_KEY, COMMENT, OLD_VALUE, NEW_VALUE, CUSTOM_1, CUSTOM_2, CUSTOM_3, CUSTOM_4, OLD_DATA, NEW_DATA) VALUES
-- BUSINESS_PROCESS_ID, PARENT_BUSINESS_PROCESS_ID, TASK_ID, 	                                    EVENT_TYPE, CREATED, 		                        USER_ID,    DOMAIN, 	    WORKBASKET_KEY, 							POR_COMPANY	, POR_SYSTEM, POR_INSTANCE	, POR_TYPE	, POR_VALUE	, TASK_CLASSIFICATION_KEY, TASK_CLASSIFICATION_CATEGORY	, ATTACHMENT_CLASSIFICATION_KEY	, COMMENT						, OLD_VALUE	, NEW_VALUE	, CUSTOM_1	, CUSTOM_2	, CUSTOM_3	, CUSTOM_4	, OLD_DATA	, NEW_DATA
('BPI:01'	        ,'',                        'TKI:000000000000000000000000000000000000',     'CREATE', 	CURRENT_TIMESTAMP,                      'USER_2_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', 'L140101'				, 'TASK'						,''								, 'this task has been created'	,'old_val'	,'new_val'	,'custom1'	,'custom2'	, 'custom3'	,'custom4'	,'123'		,'456'),
('BPI:02'	        ,'',                        'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -2, CURRENT_TIMESTAMP),  'USER_1_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '65464564', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'777'),
('BPI:03'	        ,'BPI:01',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                        'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'119'		,'555'),
('BPI:04'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'999'),
('BPI:03'	        ,'BPI:02',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                        'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'1188'		,'QQQ'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -6, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'eee'),
('BPI:06'	        ,'BPI:04',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                        'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'ZZZ'		,'777'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -5, CURRENT_TIMESTAMP),  'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'999'),
('BPI:04'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'288'		,'ooo'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '65464564', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'SSS'),
('BPI:04'	        ,'BPI:01',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'DDD'		,'555'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -2, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '68887564', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'555'),
('BPI:03'	        ,'BPI:05',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'777'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -5, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '68887564', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'456'),
('BPI:03'	        ,'BPI:07',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'288'		,'ooo'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'456'),
('BPI:03'	        ,'BPI:07',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'PPP'		,'777'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -5, CURRENT_TIMESTAMP),  'USER_1_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'999'),
('BPI:05'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'eee'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -2, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'555'),
('BPI:04'	        ,'BPI:04',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'555'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -6, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '77887564', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'SSS'),
('BPI:03'	        ,'BPI:01',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'PPP'		,'456'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '77887500', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'777'),
('BPI:05'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'ooo'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -2, CURRENT_TIMESTAMP),  'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '77887500', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'QQQ'),
('BPI:05'	        ,'BPI:04',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'288'		,'456'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -5, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'555'),
('BPI:03'	        ,'BPI:05',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'000'		,'555'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -6, CURRENT_TIMESTAMP),  'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'777'),
('BPI:04'	        ,'BPI:07',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'PPP'		,'eee'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -5, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'999'),
('BPI:03'	        ,'BPI:01',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'PPP'		,'ooo'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'555'),
('BPI:05'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'777'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '77887500', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'SSS'),
('BPI:06'	        ,'BPI:07',                  'TKI:000000000000000000000000000000000001',     'DELETE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'QQQ'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11887500', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'456'),
('BPI:03'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'000'		,'999'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11887599', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'eee'),
('BPI:03'	        ,'BPI:04',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'aaa'		,'555'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'CREATE', 	DATEADD('DAY', -6, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'555'),
('BPI:06'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'acc'		,'ooo'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'777'),
('BPI:04'	        ,'BPI:03',                  'TKI:000000000000000000000000000000000001',     'CREATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'acc'		,'999'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_2_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11887599', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'999'),
('BPI:03'	        ,'BPI:05',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'WWW'		,'SSS'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'UPDATE', 	DATEADD('DAY', -5, CURRENT_TIMESTAMP),  'USER_2_1', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11887599', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'eee'),
('BPI:06'	        ,'BPI:05',                  'TKI:000000000000000000000000000000000001',     'UPDATE', CURRENT_TIMESTAMP,                         'USER_2_1', 'DOMAIN_A', 	'WBI:100000000000000000000000000000000002', '11'		, ''		, '22'			, ''		, ''		, 'L140101'				, 'TASK'						,'DOCTYPE_DEFAULT'				, 'created a bug'				,''			,''			,'custom1'	,''			, 'custom3'	,''			,'123'		,'555'),
('BPI:02'	        ,''		,                   'TKI:000000000000000000000000000000000000',     'DELETE', 	DATEADD('DAY', -1, CURRENT_TIMESTAMP),  'USER_1_2', 'DOMAIN_B', 	'WBI:100000000000000000000000000000000001', '00'		, 'PASystem', '00'			, 'VNR'		, '11223344', ''					, ''							,''								, 'created by Peter'			,'2old_val'	,'new_val2'	,'custom1'	,''			, 'custom2'	,''			,'234'		,'777');
