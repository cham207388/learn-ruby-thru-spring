require "test_helper"

class Api::TodosControllerTest < ActionDispatch::IntegrationTest
  test "creates a todo and returns its location" do
    assert_difference("Todo.count", 1) do
      post api_todos_url, params: { title: "Buy milk", description: "2 liters", due_date: "2026-12-01" }, as: :json
    end

    assert_response :created
    body = response.parsed_body
    assert_equal "Buy milk", body["title"]
    assert_equal "2026-12-01", body["due_date"]
    assert_equal false, body["completed"]
    assert_not_nil body["created_at"]
    assert_equal api_todo_url(body["id"]), response.location
  end

  test "rejects a blank title with problem details" do
    assert_no_difference("Todo.count") do
      post api_todos_url, params: { title: "  " }, as: :json
    end

    assert_response :unprocessable_content
    assert_equal "application/problem+json", response.media_type
    body = response.parsed_body
    assert_equal 422, body["status"]
    assert_includes body["errors"]["title"], "can't be blank"
  end

  test "rejects a title over 200 characters" do
    post api_todos_url, params: { title: "a" * 201 }, as: :json

    assert_response :unprocessable_content
    assert_not_empty response.parsed_body["errors"]["title"]
  end

  test "ignores id and completed in the request" do
    post api_todos_url, params: { title: "x", id: 999, completed: true }, as: :json

    assert_response :created
    body = response.parsed_body
    assert_not_equal 999, body["id"]
    assert_equal false, body["completed"]
  end
end
