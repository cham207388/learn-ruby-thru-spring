// 1 var and type inference
var count = 5;
Console.WriteLine($"1: {count.GetType()} {count % 2 == 0}");

// 2 string interpolation and raw strings
var name = "todo";
Console.WriteLine($"2: Hi {name.ToUpperInvariant()} | {"""{"title": "x"}"""}");

// 3 nullable reference types and null operators
string? description = null;
Console.WriteLine($"3: {description?.Length} | {description ?? "none"} | {description?.Length ?? 0}");

// 4 records: value equality and with-expressions
var a = new TodoItem("Buy milk", false);
var b = a with { Completed = true };
Console.WriteLine($"4: {a == new TodoItem("Buy milk", false)} | {b}");

// 5 LINQ = streams
int[] numbers = [1, 2, 3, 4];
var tens = numbers.Where(n => n % 2 == 0).Select(n => n * 10).ToList();
Console.WriteLine($"5: [{string.Join(", ", tens)}]");

// 6 pattern matching and switch expressions
static string Label(int? size) => size switch
{
    null => "default",
    < 1 => "too small",
    > 100 => "capped",
    _ => "ok",
};
Console.WriteLine($"6: {Label(null)} {Label(0)} {Label(500)} {Label(20)}");

// 7 dictionaries: indexer throws, TryGetValue does not
var todo = new Dictionary<string, object?> { ["title"] = "Buy milk" };
Console.WriteLine($"7: {todo.TryGetValue("due_date", out var due)} {due is null}");

// 8 async/await
var total = await CountAsync();
Console.WriteLine($"8: {total}");

// 9 extension members (C# 14)
Console.WriteLine($"9: {"  ".IsBlank} {"x".IsBlank}");

static async Task<int> CountAsync()
{
    await Task.Delay(10);
    return 42;
}

record TodoItem(string Title, bool Completed);

static class StringExtensions
{
    extension(string text)
    {
        public bool IsBlank => string.IsNullOrWhiteSpace(text);
    }
}
