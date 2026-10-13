namespace TodoApi.Todos;

public sealed class Todo
{
    public long Id { get; set; }

    public required string Title { get; set; }

    public string? Description { get; set; }

    public bool Completed { get; set; }

    public DateOnly? DueDate { get; set; }

    public DateTimeOffset CreatedAt { get; set; }

    public DateTimeOffset UpdatedAt { get; set; }
}
