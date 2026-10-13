using Microsoft.EntityFrameworkCore;
using TodoApi.Todos;

namespace TodoApi.Data;

public sealed class TodoDbContext(DbContextOptions<TodoDbContext> options) : DbContext(options)
{
    public DbSet<Todo> Todos => Set<Todo>();

    protected override void OnModelCreating(ModelBuilder modelBuilder) =>
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(TodoDbContext).Assembly);
}
