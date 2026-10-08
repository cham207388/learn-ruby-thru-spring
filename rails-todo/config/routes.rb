Rails.application.routes.draw do
  get "up" => "rails/health#show", as: :rails_health_check

  mount OasRails::Engine => "/docs"

  namespace :api do
    resources :todos, only: %i[create show]
  end
end
