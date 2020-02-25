{application, 'rabbitmq_web_stomp', [
	{description, "RabbitMQ STOMP-over-WebSockets support"},
	{vsn, "3.8.2"},
	{id, "v3.8.2-rc.1-2-ga40e67c"},
	{modules, ['rabbit_web_stomp_app','rabbit_web_stomp_connection_sup','rabbit_web_stomp_handler','rabbit_web_stomp_listener','rabbit_web_stomp_middleware','rabbit_web_stomp_sup']},
	{registered, [rabbitmq_web_stomp_sup]},
	{applications, [kernel,stdlib,cowboy,rabbit_common,rabbit,rabbitmq_stomp]},
	{mod, {rabbit_web_stomp_app, []}},
	{env, [
	    {tcp_config, [{port, 15674}]},
	    {ssl_config, []},
	    {num_tcp_acceptors, 10},
	    {num_ssl_acceptors, 10},
	    {cowboy_opts, []},
	    {proxy_protocol, false},
	    {ws_frame, text},
	    {use_http_auth, false}
	  ]},
		{broker_version_requirements, []}
]}.