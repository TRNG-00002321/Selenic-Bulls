"""
User model.
"""
from dataclasses import dataclass
from typing import Optional


@dataclass
class User:
    id: Optional[int]
    username: str
    password: str
    role: str
    
    def __post_init__(self):
        if self.role != 'Employee':
            raise ValueError("Role must be 'Employee'")