using Microsoft.EntityFrameworkCore;
using Scalar.AspNetCore;
using TodoApi.Data;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<TodoDbContext>(options =>
    options
        .UseNpgsql(builder.Configuration.GetConnectionString("Todo"))
        .UseSnakeCaseNamingConvention());

builder.Services.AddProblemDetails();
builder.Services.AddOpenApi();
builder.Services.AddHealthChecks();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.MapScalarApiReference();
}

app.MapHealthChecks("/health");

app.Run();
