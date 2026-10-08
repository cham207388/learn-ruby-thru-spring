class ApplicationController < ActionController::API
  private

  # Renders an RFC 9457 Problem Details response, same shape as Spring's ProblemDetail
  def render_problem(status:, detail:, errors: nil)
    code = Rack::Utils.status_code(status)
    body = {
      title: Rack::Utils::HTTP_STATUS_CODES[code],
      status: code,
      detail: detail,
      instance: request.path
    }
    body[:errors] = errors if errors
    render json: body, status: code, content_type: "application/problem+json"
  end
end
