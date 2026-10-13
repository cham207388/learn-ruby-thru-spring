# frozen_string_literal: true

class Todo < ApplicationRecord
  validates :title, presence: true, length: { maximum: 200 }
  scope :by_completed, ->(value) { where(completed: value) }
end
