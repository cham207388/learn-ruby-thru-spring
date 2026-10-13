class Api::TodosController < ApplicationController
  include Pagy::Method

  # @summary List todos, newest first
  # @tags Todos
  # @parameter completed(query) [Boolean] Filter by completion; omit for all
  # @parameter page(query) [Integer] 1-based page number
  # @parameter size(query) [Integer] Page size, 1 to 100
  # @response Page of todos(200) [Hash{items: Array<Todo>, page: Integer, size: Integer, total: Integer}]
  def index
    completed = ActiveModel::Type::Boolean.new.cast(params[:completed])
    todos = Todo.order(created_at: :desc, id: :desc)
    todos = todos.by_completed(completed) unless completed.nil?

    pagy, items = pagy(:offset, todos, limit: 20, client_limit: 100, limit_key: "size")

    render json: { items:, page: pagy.page, size: pagy.limit, total: pagy.count }
  end

  # @summary Create a todo
  # @tags Todos
  # @request_body The todo to create [!Hash{title: String, description: String, due_date: String}]
  # @response Created; Location header points to the new todo(201) [Todo]
  # @response Validation failed(422) [Hash{title: String, status: Integer, detail: String, instance: String, errors: Hash}]
  def create
    todo = Todo.new(create_params)

    if todo.save
      render json: todo, status: :created, location: api_todo_url(todo)
    else
      render_problem(status: :unprocessable_content, detail: "Validation failed", errors: todo.errors.to_hash)
    end
  end

  # @summary Get one todo
  # @tags Todos
  # @response The todo(200) [Todo]
  # @response No todo with this id(404) [Hash{title: String, status: Integer, detail: String, instance: String}]
  def show
    render json: Todo.find(params[:id])
  end

  private

  def create_params
    params.expect(todo: [ :title, :description, :due_date ])
  end
end
