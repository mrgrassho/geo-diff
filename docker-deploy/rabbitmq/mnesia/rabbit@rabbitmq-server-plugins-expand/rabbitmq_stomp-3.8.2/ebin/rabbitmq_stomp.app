{application, 'rabbitmq_stomp', [
	{description, "RabbitMQ STOMP plugin"},
	{vsn, "3.8.2"},
	{id, "v3.8.2-rc.1-2-ga40e67c"},
	{modules, ['Elixir.RabbitMQ.CLI.Ctl.Commands.ListStompConnectionsCommand','rabbit_stomp','rabbit_stomp_client_sup','rabbit_stomp_connection_info','rabbit_stomp_frame','rabbit_stomp_processor','rabbit_stomp_reader','rabbit_stomp_sup','rabbit_stomp_util']},
	{registered, [rabbitmq_stomp_sup]},
	{applications, [kernel,stdlib,ranch,rabbit_common,rabbit,amqp_client]},
	{mod, {rabbit_stomp, []}},
	{env, [
	    {default_user,
	     [{login, <<"guest">>},
	      {passcode, <<"guest">>}]},
	    {default_vhost, <<"/">>},
	    {default_topic_exchange, <<"amq.topic">>},
	    {ssl_cert_login, false},
	    {implicit_connect, false},
	    {tcp_listeners, [61613]},
	    {ssl_listeners, []},
	    {num_tcp_acceptors, 10},
	    {num_ssl_acceptors, 10},
	    {tcp_listen_options, [{backlog,   128},
	                          {nodelay,   true}]},
	    %% see rabbitmq/rabbitmq-stomp#39
	    {trailing_lf, true},
	    %% see rabbitmq/rabbitmq-stomp#57
	    {hide_server_info, false},
	    {proxy_protocol, false}
	  ]},
		{broker_version_requirements, []}
]}.