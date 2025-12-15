from operator import contains

import pytest
from Calculator import Calculator


#Test path (CLI): pytest .\test\test_pytest_basics.py -v

@pytest.fixture
def calculator():
    calc = Calculator()
    yield calc


@pytest.mark.parametrize("val1,val2, result",[
    (1,2,3),
    (2,4,6),
    (3,6,9),
    (4,8,12),
    (5,10,15)
])
def test_calculator(calculator,val1,val2,result):
    #calculator = Calculator()
    aresult = calculator.add(val1, val2)
    assert aresult == result


def test_iseven(calculator):
    #calculator = Calculator()
    assert calculator.is_even(2) is True
    assert not calculator.is_even(3)


def test_subtract(calculator):
    #calculator = Calculator()
    result = calculator.subtract(1, 2)
    assert result == -1

# marker test path(CLI): pytest -m <marker name> -v
@pytest.mark.DivideTests
def test_divide_by_zero(calculator):
    #calculator = Calculator()
    with pytest.raises(ZeroDivisionError) as e:
        calculator.divide(9, 0)

@pytest.mark.DivideTests
def test_divide_by_zero_context(calculator):
    #calculator = Calculator()
    with pytest.raises(ZeroDivisionError) as e:
        calculator.divide(5, 0)
    assert contains(str(e.value), 'zero')
    #assert 'zero' in str(e.value).lower() <-------- same thing as above
    #assert str(e.value) == 'Cannot divide by zero'

@pytest.mark.parametrize("val, result",[
    (1,2),
    (2,4),
    (3,6),
    (4,8),
    (5,10)
])
def test_multiply(calculator,val, result):
    aresult = calculator.multiply(val, 2)
    assert aresult == result

