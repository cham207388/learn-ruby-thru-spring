# frozen_string_literal: true

require "test_helper"

class HealthTest < ActionDispatch::IntegrationTest
  test "health check returns 200" do
    get rails_health_check_path
    assert_response :success
  end
end
