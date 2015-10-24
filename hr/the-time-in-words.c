int main() {
  int hour, minute;
  scanf("%d%d", &hour, &minute);
  printf("%s\n", CCL_HR_time_to_words(hour, minute)->value.as_str.buffer);
}
