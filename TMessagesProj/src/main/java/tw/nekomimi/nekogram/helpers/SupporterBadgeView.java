package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

/**
 * Small helper view that renders the custom supporter badge drawable and provides
 * a consistent tap target that shows the supporter toast.
 */
public class SupporterBadgeView extends View {

    private final SupporterBadgeDrawable drawable = new SupporterBadgeDrawable();
    private long dialogId;

    public SupporterBadgeView(Context context) {
        super(context);
        init();
    }

    public SupporterBadgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SupporterBadgeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVisibility(GONE);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setOnClickListener(v -> SupporterBadgeHelper.showInfo(getContext()));
        setClickable(true);
    }

    public void setDialogId(long dialogId) {
        this.dialogId = dialogId;
        boolean show = SupporterBadgeHelper.hasBadge(dialogId);
        setVisibility(show ? VISIBLE : GONE);
        invalidate();
    }

    public long getDialogId() {
        return dialogId;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = AndroidUtilities.dp(22);
        int resolvedWidth = resolveSize(size, widthMeasureSpec);
        int resolvedHeight = resolveSize(size, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
    }

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        if (getVisibility() != VISIBLE) {
            return;
        }
        drawable.setBounds(0, 0, getWidth(), getHeight());
        drawable.draw(canvas);
    }
}
