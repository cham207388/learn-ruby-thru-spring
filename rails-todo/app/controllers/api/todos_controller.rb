class Api::TodosController < ApplicationController
  include Pagy::Method

  before_action :set_todo, only: %i[show update]

  # @summary List todos, newest first
  # @tags Todos
  def index
    completed = ActiveModel::Type::Boolean.new.cast(params[:completed])
    todos = Todo.order(created_at: :desc, id: :desc)
    todos = todos.by_completed(completed) unless completed.nil?

    pagy, items = pagy(:offset, todos, limit: 20, client_limit: 100, limit_key: "size")

    render json: { items:, page: pagy.page, size: pagy.limit, total: pagy.count }
  end

  # @summary Get one todo
  # @tags Todos
  # @response The todo(200) [Todo]
  # @response No todo with this id(404) [Hash{title: String, status: Integer, detail: String, instance: String}]
  def show
    render json: @todo
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

  # @summary Update a todo (only the fields sent)
  # @tags Todos
  # @request_body Fields to change [!Hash{title: String, description: String, due_date: String, completed: Boolean}]
  # @response The updated todo(200) [Todo]
  # @response No todo with this id(404) [Hash{title: String, status: Integer, detail: String, instance: String}]
  # @response Validation failed(422) [Hash{title: String, status: Integer, detail: String, instance: String, errors: Hash}]
  def update
    if @todo.update(update_params)
      render json: @todo
    else
      render_problem(status: :unprocessable_content, detail: "Validation failed", errors: @todo.errors.to_hash)
    end
  end

  private

  # Runs before show and update; an unknown id raises RecordNotFound, handled in ApplicationController
  def set_todo
    @todo = Todo.find(params[:id])
  end

  def create_params
    params.expect(todo: [ :title, :description, :due_date ])
  end

  # Update may also change completed; create may not
  def update_params
    params.expect(todo: [ :title, :description, :due_date, :completed ])
  end
end
