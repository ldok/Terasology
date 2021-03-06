/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.nui.layout;

import org.terasology.input.MouseInput;
import org.terasology.math.Rect2i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector2i;
import org.terasology.rendering.nui.BaseInteractionListener;
import org.terasology.rendering.nui.Border;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.CoreLayout;
import org.terasology.rendering.nui.InteractionListener;
import org.terasology.rendering.nui.SubRegion;
import org.terasology.rendering.nui.UIWidget;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Immortius
 */
public class ScrollableArea extends CoreLayout {
    private UIWidget content;
    private int contentHeight;
    private int scrollbarWidth = 16;

    private int offset;
    private boolean dragging;

    private int sliderHeight;
    private int canvasHeight;

    private InteractionListener handleListener = new BaseInteractionListener() {
        private int mouseOffset;


        @Override
        public void onMouseDrag(Vector2i pos) {
            int newPosition = TeraMath.clamp(pos.y - mouseOffset, 0, sliderHeight);

            offset = newPosition * (contentHeight - canvasHeight) / sliderHeight;
        }

        @Override
        public boolean onMouseClick(MouseInput button, Vector2i pos) {
            if (button == MouseInput.MOUSE_LEFT) {
                dragging = true;
                mouseOffset = pos.y - pixelOffsetFor(offset);
                return true;
            }
            return false;
        }

        @Override
        public void onMouseRelease(MouseInput button, Vector2i pos) {
            dragging = false;
        }
    };

    @Override
    public void onDraw(Canvas canvas) {
        if (canvas.size().y < contentHeight) {
            Border margin = canvas.getCurrentStyle().getMargin();

            // Draw content
            Rect2i contentRegion = margin.shrink(Rect2i.createFromMinAndSize(0, 0, canvas.size().x - scrollbarWidth, canvas.size().y));
            try (SubRegion ignored = canvas.subRegion(contentRegion, true)) {
                canvas.drawElement(content, Rect2i.createFromMinAndSize(0, -offset, canvas.size().x, contentHeight));
            }
            try (SubRegion ignored = canvas.subRegion(Rect2i.createFromMinAndSize(canvas.size().x - scrollbarWidth - margin.getRight(), margin.getTop(),
                    scrollbarWidth, canvas.size().y - margin.getTotalHeight()), false)) {
                canvasHeight = canvas.size().y;
                canvas.setPart("scrollbar-slider");
                canvas.drawBackground();

                canvas.setPart("scrollbar-handle");
                sliderHeight = canvasHeight - canvas.getCurrentStyle().getFixedHeight();
                int drawLocation = pixelOffsetFor(offset);
                Rect2i handleRegion = Rect2i.createFromMinAndSize(0, drawLocation, canvas.getCurrentStyle().getFixedWidth(), canvas.getCurrentStyle().getFixedHeight());
                canvas.drawBackground(handleRegion);
                canvas.addInteractionRegion(handleListener, handleRegion);
            }
        } else {
            canvas.drawElement(content, Rect2i.createFromMinAndSize(0, 0, canvas.size().x, contentHeight));
        }
    }

    private int pixelOffsetFor(int value) {
        return sliderHeight * value / (contentHeight - canvasHeight);
    }

    public void setContent(UIWidget widget) {
        this.content = widget;
    }

    public void setContentHeight(int height) {
        this.contentHeight = height;
    }

    public int getScrollbarWidth() {
        return scrollbarWidth;
    }

    public void setScrollbarWidth(int width) {
        this.scrollbarWidth = width;
    }

    @Override
    public String getMode() {
        if (dragging) {
            return ACTIVE_MODE;
        } else if (handleListener.isMouseOver()) {
            return HOVER_MODE;
        }
        return DEFAULT_MODE;
    }

    @Override
    public Iterator<UIWidget> iterator() {
        return Arrays.asList(content).iterator();
    }
}
