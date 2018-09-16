# spring-data-query-helper
Dynamically build spring data query statements


## Build native query
```java
public Page<User> searchUser(UserParam param, Pageable pageable) {
    NativeQueryMetadata md = new NativeQueryMetadata();
    
    md.setSelect("u.user_id, u.user_name, u.user_age, u.user_category, a.account_no");
    
    md.setFrom("user_table u");
    
    md.addJoin("inner join user_account a on u.user_id = a.user_id");
    
    // whether add the left join depends on param.getUserInfoAddress() is null or not
    md.addJoin("left join user_info i on u.user_id = i.user_id", param.getUserInfoAddress());
    
    // the order by can be overrided by sort of pageable
    md.addOrderBy("u.user_age");
    md.addOrderBy("a.acount_no");
    
    // query result will be mapped to User
    md.setResultClass(User.class);
    
    // "u.user_name = ?" works when param.getUsername() is not null
    md.addWhere("u.user_name = ?", param.getUsername());
    md.addWhere("u.user_category = ?", param.getUserCategory());
    md.addWhere("i.address = ?", param.getUserInfoAddress());
    
    return nativeQueryHelper.query(md, pageable);
}
```
