"""
Calculator module - System Under Test for Python testing demos.
"""


class Calculator:
    """A simple calculator class for demonstration purposes."""

    def add(self, a: int, b: int) -> int:
        """Add two numbers."""
        return a + b

    def subtract(self, a: int, b: int) -> int:
        """Subtract b from a."""
        return a - b

    def multiply(self, a: int, b: int) -> int:
        """Multiply two numbers."""
        return a * b

    def divide(self, a: int, b: int) -> float:
        """
        Divide a by b.

        Raises:
            ZeroDivisionError: If b is zero
        """
        if b == 0:
            raise ZeroDivisionError("Cannot divide by zero")
        return a / b

    def modulo(self, a: int, b: int) -> int:
        """
        Calculate a modulo b.

        Raises:
            ZeroDivisionError: If b is zero
        """
        if b == 0:
            raise ZeroDivisionError("Cannot calculate modulo with zero divisor")
        return a % b

    def power(self, base: int, exponent: int) -> int:
        """
        Calculate base raised to exponent.

        Raises:
            ValueError: If exponent is negative
        """
        if exponent < 0:
            raise ValueError("Exponent cannot be negative for integer power")
        return base ** exponent

    def absolute_value(self, value: int) -> int:
        """Return the absolute value."""
        return abs(value)

    def is_even(self, number: int) -> bool:
        """Check if a number is even."""
        return number % 2 == 0

    def is_positive(self, number: int) -> bool:
        """Check if a number is positive (greater than zero)."""
        return number > 0

    def max_value(self, a: int, b: int) -> int:
        """Return the larger of two numbers."""
        return max(a, b)

    def min_value(self, a: int, b: int) -> int:
        """Return the smaller of two numbers."""
        return min(a, b)

    """Mathematical operations for testing practice."""

    def factorial(self, n: int) -> int:
        """Calculate factorial of n."""
        if n < 0:
            raise ValueError("Factorial not defined for negative numbers")
        if n <= 1:
            return 1
        return n * self.factorial(n - 1)

    def fibonacci(self, n: int) -> int:
        """Return the nth Fibonacci number (0-indexed)."""
        if n < 0:
            raise ValueError("Fibonacci not defined for negative indices")
        if n <= 1:
            return n
        a, b = 0, 1
        for _ in range(2, n + 1):
            a, b = b, a + b
        return b

    def is_prime(self, n: int) -> bool:
        """Check if n is a prime number."""
        if n < 2:
            return False
        if n == 2:
            return True
        if n % 2 == 0:
            return False
        for i in range(3, int(n ** 0.5) + 1, 2):
            if n % i == 0:
                return False
        return True

    def get_primes_up_to(self, n: int) -> list:
        """Return list of primes up to n."""
        return [x for x in range(2, n + 1) if self.is_prime(x)]

    def gcd(self, a: int, b: int) -> int:
        """Calculate greatest common divisor."""
        a, b = abs(a), abs(b)
        while b:
            a, b = b, a % b
        return a


class StringCalculator:
    """Calculator that works with string number inputs."""

    def add(self, numbers: str) -> int:
        """
        Add numbers from a comma-separated string.

        Args:
            numbers: Comma-separated string of numbers (e.g., "1,2,3")

        Returns:
            Sum of all numbers, or 0 if empty/None
        """
        if not numbers or numbers.strip() == "":
            return 0

        parts = numbers.split(",")
        return sum(int(p.strip()) for p in parts if p.strip())
