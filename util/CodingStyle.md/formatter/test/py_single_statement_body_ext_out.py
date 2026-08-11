if (
    enabled
): run()
next_step()

if ready: result = (
        compute()
    )
next_step()

if logged: record()  # Retain this
next_step()

if condition:
    perform_a_very_long_operation_with_a_descriptive_name_and_many_arguments(first, second, third)
next_step()

if chained:
    first(); second()
next_step()
