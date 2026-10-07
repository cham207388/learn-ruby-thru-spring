# The US-1.3 irb drill as a script. Run from repo root:
#   ruby playground/irb_drill.rb
#
# No class or main method is required: Ruby runs a file top to bottom.
# `p` prints the inspected value (like debugger output); `puts` prints plain text.

# Prints a label, then the inspected result of the block.
def show(label)
  puts "#{label.ljust(45)} => #{yield.inspect}"
end

def done?(todo) = todo[:completed]

def build(title:, completed: false) = { title:, completed: }

def main
  puts "-- 1. Everything is an object"
  show("5.class") { 5.class }
  show("nil.class") { nil.class }
  show("5.even?") { 5.even? }

  puts "\n-- 2. String interpolation"
  name = "todo"
  show('"Hi #{name.upcase}"') { "Hi #{name.upcase}" }
  show("'Hi \#{name}'") { 'Hi #{name}' }

  puts "\n-- 3. Symbols vs strings"
  show(":title.object_id == :title.object_id") { :title.object_id == :title.object_id }
  show('"title".object_id == "title".object_id') { "title".object_id == "title".object_id }

  puts "\n-- 4. Hash keys: symbol vs string"
  todo = { title: "Buy milk", completed: false }
  show("todo[:title]") { todo[:title] }
  show('todo["title"]') { todo["title"] }

  puts "\n-- 5. fetch fails loud (begin/rescue = try/catch)"
  begin
    todo.fetch(:due_date)
  rescue KeyError => e
    puts "todo.fetch(:due_date)".ljust(45) + " raised #{e.class}: #{e.message}"
  end
  show("todo.fetch(:due_date, nil)") { todo.fetch(:due_date, nil) }

  puts "\n-- 6. Safe navigation"
  show("todo[:due_date]&.year") { todo[:due_date]&.year }

  puts "\n-- 7. Truthiness: only nil and false are falsy"
  zero = 0
  nothing = nil
  show('if 0 then "truthy" end') { "truthy" if zero }
  show('if nil then "x" else "falsy" end') { nothing ? "x" : "falsy" }

  puts "\n-- 8. Blocks replace lambdas"
  show("select { |n| n.even? }.map { |n| n * 10 }") { [1, 2, 3, 4].select { |n| n.even? }.map { |n| n * 10 } }

  puts "\n-- 9. Symbol to proc"
  show("[1, 2, 3, 4].select(&:even?)") { [1, 2, 3, 4].select(&:even?) }

  puts "\n-- 10. Hash iteration"
  todo.each { |key, value| puts "  #{key}: #{value}" }

  puts "\n-- 11. Endless method, ? naming"
  show("done?(todo)") { done?(todo) }

  puts "\n-- 12. Keyword arguments with defaults"
  show('build(title: "x")') { build(title: "x") }
end

# Same role as `public static void main`: runs only when this file is executed
# directly, not when another file loads it with `require`.
main if __FILE__ == $PROGRAM_NAME
