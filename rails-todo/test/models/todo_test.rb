require "test_helper"

class TodoTest < ActiveSupport::TestCase
  test "saves with a valid title and completed defaults to false" do
    todo = Todo.new(title: "Write tests")

    assert todo.save
    assert_not todo.completed
    assert_not_nil todo.created_at
  end

  test "is invalid with a blank title" do
    todo = Todo.new(title: "  ")

    assert_not todo.valid?
    assert_includes todo.errors[:title], "can't be blank"
  end

  test "is invalid with a title over 200 characters" do
    todo = Todo.new(title: "a" * 201)

    assert_not todo.valid?
    assert_includes todo.errors[:title], "is too long (maximum is 200 characters)"
  end

  test "accepts a title of exactly 200 characters" do
    assert Todo.new(title: "a" * 200).valid?
  end

  test "fixture buy_milk loads with its title" do
    todo = todos(:buy_milk)

    assert_equal "Buy milk", todo.title
    assert_not todo.completed
  end
end
