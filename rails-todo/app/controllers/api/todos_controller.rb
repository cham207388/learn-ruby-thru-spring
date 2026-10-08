class Api::TodosController < ApplicationController

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

  private

  def create_params
    params.expect(todo: [ :title, :description, :due_date ])
  end
end
