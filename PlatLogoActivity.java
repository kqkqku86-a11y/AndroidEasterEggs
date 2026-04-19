// Update the initial level from 10000 to 0 in the onCreate method
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Other initialization code

    // Change initial level to fix bubble visibility issue
    bubble.setLevel(0); // Changed from 10000 to 0
}
