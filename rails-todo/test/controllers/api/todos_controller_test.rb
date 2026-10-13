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

  test "lists todos newest first with page metadata" do
    get api_todos_url

    assert_response :ok
    body = response.parsed_body
    assert_equal [ "File taxes", "Buy milk" ], body["items"].map { |todo| todo["title"] }
    assert_equal 1, body["page"]
    assert_equal 20, body["size"]
    assert_equal 2, body["total"]
  end

  test "filters by completed" do
    get api_todos_url, params: { completed: "true" }
    assert_equal [ "File taxes" ], response.parsed_body["items"].map { |todo| todo["title"] }

    get api_todos_url, params: { completed: "false" }
    assert_equal [ "Buy milk" ], response.parsed_body["items"].map { |todo| todo["title"] }
  end

  test "paginates with one-based pages" do
    get api_todos_url, params: { page: 2, size: 1 }

    body = response.parsed_body
    assert_equal [ "Buy milk" ], body["items"].map { |todo| todo["title"] }
    assert_equal 2, body["page"]
    assert_equal 1, body["size"]
    assert_equal 2, body["total"]
  end

  test "clamps size to 100" do
    get api_todos_url, params: { size: 500 }

    assert_equal 100, response.parsed_body["size"]
  end

  test "rejects a negative size with 400 problem details" do
    get api_todos_url, params: { size: -1 }

    assert_response :bad_request
    assert_equal "application/problem+json", response.media_type
  end

  test "shows one todo" do
    get api_todo_url(todos(:buy_milk))

    assert_response :ok
    assert_equal "Buy milk", response.parsed_body["title"]
  end

  test "returns 404 problem details for an unknown id" do
    get api_todo_url(id: 999_999)

    assert_response :not_found
    assert_equal "application/problem+json", response.media_type
    assert_equal 404, response.parsed_body["status"]
  end
end
