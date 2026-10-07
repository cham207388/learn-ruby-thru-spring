class CreateTodos < ActiveRecord::Migration[8.1]
  def change
    create_table :todos do |t|
      t.string :title, null: false, limit: 200
      t.text :description
      t.boolean :completed, null: false, default: false
      t.date :due_date

      t.timestamps
    end
  end
end
