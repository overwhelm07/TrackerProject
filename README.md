# Tracker
User Activity Tracker
사용자의 활동량과 체류 장소를 에너지 효율적으로 모니터링 하여 기록하는 모바일 센싱 애플리케이션을 구현

Task
1.활동량
-걸음수, 이동(움직임)시간, 정지(비움직임)시간
2.체류 장소
-장소 이름 or 실내/외
-체류 시간
3.각 이벤트 시각
4.날짜

활동량
움직임/정지 (1분 이상 움직임이 있는 경우 기록)

체류 장소
실내 2곳, 실외 2곳 4개의 장소에 대해서 기록
그 외의 체류 장소는 실내/실외로만 구분
5분 이상 체류한 경우 체류 장소로 판단하고 기록


Do it(시작시간-종료시간 during 이동 걸음걸이)
어댑티브 센싱
일정 주기에 움직임이 센싱이되면 빈도를 높여서 센싱 주기를 높임.
1 <-> 5 <-> 10(기본) -> 15 -> 20


