# frozen_string_literal: true

OasRails.configure do |config|
  config.info.title = "Todo API (Rails)"
  config.info.version = "v1"
  config.info.summary = "Same contract as the Spring app: snake_case JSON, RFC 9457 Problem Details"
  config.servers = [ { url: "http://localhost:3000", description: "Local" } ]
  config.tags = [ { name: "Todos", description: "Create and manage todos" } ]
  config.api_path = "/api"
  config.authenticate_all_routes_by_default = false
  config.set_default_responses = false
end
