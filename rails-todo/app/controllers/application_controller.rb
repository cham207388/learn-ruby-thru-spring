class ApplicationController < ActionController::API
  rescue_from ActiveRecord::RecordNotFound, with: :not_found
  rescue_from Pagy::OptionError, with: :bad_request

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

  # Any controller that calls find with an unknown id ends up here
  def not_found(error)
    render_problem(status: :not_found, detail: error.message)
  end

  # Pagy rejects values it cannot page with, such as ?size=-1
  def bad_request(error)
    render_problem(status: :bad_request, detail: error.message)
  end
end
