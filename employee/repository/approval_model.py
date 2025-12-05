"""
Approval model.
"""
from dataclasses import dataclass
from typing import Optional


@dataclass
class Approval:
    id: Optional[int]
    expense_id: int
    status: str
    reviewer: Optional[int]
    comment: Optional[str]
    review_date: Optional[str]
    
    def __post_init__(self):
        if self.status not in ['pending', 'approved', 'denied']:
            raise ValueError("Status must be 'pending', 'approved', or 'denied'")