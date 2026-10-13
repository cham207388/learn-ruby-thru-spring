using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace TodoApi.Todos;

public sealed class TodoConfiguration : IEntityTypeConfiguration<Todo>
{
    public void Configure(EntityTypeBuilder<Todo> builder)
    {
        builder.Property(todo => todo.Title).HasMaxLength(200);
        builder.Property(todo => todo.Completed).HasDefaultValue(false);
    }
}
