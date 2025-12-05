"""
Expense model.
"""
from dataclasses import dataclass
from typing import Optional


@dataclass
class Expense:
    id: Optional[int]
    user_id: int
    amount: float
    description: str
    date: str