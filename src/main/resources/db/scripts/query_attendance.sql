
select
    a1_0.id,
    a1_0.created_at,
    a1_0.created_by,
    a1_0.gbs_id,
    a1_0.member_id,
    a1_0.ministry,
    a1_0.qt_count,
    a1_0.updated_at,
    a1_0.week_start,
    a1_0.worship 
from
    attendance a1_0 
where
    a1_0.gbs_id=1;
    -- and a1_0.week_start='2025-01-01';


select
    vl1_0.user_id,
    vl1_0.created_at,
    vl1_0.end_dt,
    vl1_0.start_dt,
    vl1_0.updated_at,
    vl1_0.village_id 
from
    village_leader vl1_0
where
    vl1_0.user_id=3;


select * from gbs_leader_history where gbs_id=1;